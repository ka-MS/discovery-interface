package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import com.itmsg.device42.integration.ci.FilesystemCiIntegrate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CiRelationJobTest {

    @Test
    void definesEveryRelationPlannedForThisStage() {
        assertThat(CiRelationSource.values()).containsExactly(
                CiRelationSource.OS_INSTALLED_ON_COMPUTER,
                CiRelationSource.COMPUTER_CONTAINS_DISK,
                CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM,
                CiRelationSource.HOST_VIRTUALIZES_VM,
                CiRelationSource.DB_INSTANCE_RUNS_ON_DEVICE);
        assertThat(CiRelationSource.COMPUTER_CONTAINS_DISK.relationNum()).isEqualTo("RELATION.CONTAINS");
        assertThat(CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM.relationNum()).isEqualTo("RELATION.CONTAINS");
        assertThat(CiRelationSource.HOST_VIRTUALIZES_VM.relationNum()).isEqualTo("VIRTUALIZES");
        assertThat(CiRelationSource.DB_INSTANCE_RUNS_ON_DEVICE.relationNum()).isEqualTo("RELATION.RUNSON");
    }

    @Test
    void diskRelationKeepsHardDiskFilter() {
        assertThat(CiRelationSource.COMPUTER_CONTAINS_DISK.pageQuery(0, 10))
                .contains("pm.type_name = 'Hard Disk'")
                .contains("'D42:DEVICE:'")
                .contains("'D42:PART:'");
        assertThat(CiRelationSource.COMPUTER_CONTAINS_DISK.countQuery())
                .contains("pm.type_name = 'Hard Disk'");
    }

    @Test
    void filesystemRelationExpandsDeviceArrayAndReusesExcludedTypes() {
        String page = CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM.pageQuery(0, 10);
        assertThat(page)
                .contains("ANY(m.device_fks)")
                .doesNotContain("DISTINCT ON")
                .contains("'D42:MOUNTPOINT:'");
        assertThat(CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM.countQuery())
                .contains("ANY(m.device_fks)")
                .doesNotContain("EXISTS");
        for (String excluded : FilesystemCiIntegrate.EXCLUDED_TYPES) {
            assertThat(page).as("제외 타입 %s", excluded).contains("'" + excluded + "'");
        }
    }

    @Test
    void hostVirtualizesVmUsesVirtualHostFkAndReturnsHostToVmDirection() {
        String page = CiRelationSource.HOST_VIRTUALIZES_VM.pageQuery(0, 10);

        assertThat(page)
                .contains("host.device_pk = vm.virtual_host_device_fk")
                .contains("CAST(host.device_pk AS varchar) AS sourceci")
                .contains("CAST(vm.device_pk AS varchar) AS targetci")
                .contains("vm.type = 'virtual'")
                .doesNotContain("vm_manager_device_fk")
                .doesNotContain("host_chassis_device_fk");
        assertThat(CiRelationSource.HOST_VIRTUALIZES_VM.countQuery())
                .contains("host.device_pk = vm.virtual_host_device_fk")
                .contains("COUNT(*)");
    }

    @Test
    void dbInstanceRelationGoesThroughComponentButStoresInstanceToDevice() {
        String page = CiRelationSource.DB_INSTANCE_RUNS_ON_DEVICE.pageQuery(0, 10);

        assertThat(page)
                .contains("a.appcomp_pk = i.appcomp_fk")
                .contains("c.device_pk = a.device_fk")
                .contains("CAST(i.databaseinstance_pk AS varchar) AS sourceci")
                .contains("CAST(c.device_pk AS varchar) AS targetci")
                .doesNotContain("D42:APPCOMP:")
                .doesNotContain("host_name");
        assertThat(CiRelationSource.DB_INSTANCE_RUNS_ON_DEVICE.countQuery())
                .contains("a.appcomp_pk = i.appcomp_fk")
                .contains("d.device_pk = a.device_fk")
                .contains("COUNT(*)");
    }

    @Test
    void everyPageQueryOrdersByRelationKeyAndPages() {
        for (CiRelationSource source : CiRelationSource.values()) {
            assertThat(source.pageQuery(20, 10))
                    .as("%s 페이지 SQL", source)
                    .contains("AS sourceci")
                    .contains("AS targetci")
                    .contains("ORDER BY sourceci, targetci")
                    .contains("LIMIT 10 OFFSET 20");
            assertThat(source.countQuery()).as("%s 건수 SQL", source).contains("COUNT(*)");
            assertThat(source.relationNum()).as("%s 관계 코드", source)
                    .isIn("RELATION.INSTALLEDON", "RELATION.CONTAINS", "VIRTUALIZES", "RELATION.RUNSON");
        }
    }

    @Test
    void readsSinglePageAndReportsLoadedCount() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        stubCount(factory, 2);
        var writer = mock(ActCiRelationWriter.class);
        when(writer.write(anyList())).thenReturn(2);

        new CiRelationJob(factory, writer).run();

        var captured = ArgumentCaptor.forClass(List.class);
        verify(writer, atLeastOnce()).write(captured.capture());
        assertThat(captured.getAllValues().getFirst())
                .containsExactly(new ActCiRelationUpsert(
                        "D42:DEVICEOS:147", "D42:DEVICE:173", "RELATION.INSTALLEDON"));
    }

    @Test
    void readsEveryPageWhenSourceSpansMultipleBatches() throws Exception {
        int batchSize = CiRelationJob.DEFAULT_BATCH_SIZE;
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        var osCountRs = mock(ResultSet.class);
        when(osCountRs.next()).thenReturn(true, false);
        when(osCountRs.getLong(1)).thenReturn(batchSize + 1L);

        var zeroCountRs = mock(ResultSet.class);
        when(zeroCountRs.next()).thenReturn(true, false);
        when(zeroCountRs.getLong(1)).thenReturn(0L);

        var firstPageRs = mock(ResultSet.class);
        when(firstPageRs.next()).thenReturn(true, false);
        when(firstPageRs.getString("sourceci")).thenReturn("D42:DEVICEOS:1");
        when(firstPageRs.getString("targetci")).thenReturn("D42:DEVICE:1");

        var secondPageRs = mock(ResultSet.class);
        when(secondPageRs.next()).thenReturn(true, false);
        when(secondPageRs.getString("sourceci")).thenReturn("D42:DEVICEOS:2");
        when(secondPageRs.getString("targetci")).thenReturn("D42:DEVICE:2");

        when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.<String>getArgument(0);
            if (sql.contains("COUNT(*)")) {
                return sql.contains("view_deviceos_v1") ? osCountRs : zeroCountRs;
            }
            return sql.contains("OFFSET " + batchSize) ? secondPageRs : firstPageRs;
        });

        var writer = mock(ActCiRelationWriter.class);
        when(writer.write(anyList())).thenReturn(1);

        new CiRelationJob(factory, writer).run();

        // OS 소스만 배치 크기보다 1건 많아 두 페이지가 돌고,
        // Disk·Filesystem·Host→VM은 0건이라 write()를 부르지 않는다.
        verify(writer, times(2)).write(anyList());

        var executedQueries = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).executeQuery(executedQueries.capture());
        assertThat(executedQueries.getAllValues())
                .as("두 번째 페이지 SQL이 남은 건수만 조회한다")
                .anyMatch(sql -> sql.contains("LIMIT 1 OFFSET " + batchSize));
    }

    @Test
    void skipsWriterWhenSourceHasNoRows() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        stubCount(factory, 0);
        var writer = mock(ActCiRelationWriter.class);

        new CiRelationJob(factory, writer).run();

        verifyNoInteractions(writer);
    }

    @Test
    void continuesAfterOneRelationSourceFails() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        when(factory.openConnection()).thenThrow(new IllegalStateException("device42 down"));
        var writer = mock(ActCiRelationWriter.class);

        new CiRelationJob(factory, writer).run();

        verify(factory, times(CiRelationSource.values().length)).openConnection();
        verifyNoInteractions(writer);
    }

    /**
     * continuesAfterOneRelationSourceFails 는 실패한 소스 자신의 예외가 잡힌다는 것만 증명한다.
     * 이 테스트는 첫 소스가 실패해도 뒤 소스가 실제로 실행되어 데이터를 적재한다는 것을 증명한다.
     */
    @Test
    void continuesToLaterRelationSourcesAfterFirstSourceFailsToOpenConnection() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(factory.openConnection())
                .thenThrow(new IllegalStateException("device42 down"))
                .thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        var countRs = mock(ResultSet.class);
        when(countRs.next()).thenReturn(true, false);
        when(countRs.getLong(1)).thenReturn(1L);

        var pageRs = mock(ResultSet.class);
        when(pageRs.next()).thenReturn(true, false);
        when(pageRs.getString("sourceci")).thenReturn("D42:DEVICE:1");
        when(pageRs.getString("targetci")).thenReturn("D42:PART:1");

        when(statement.executeQuery(anyString())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).contains("COUNT(*)") ? countRs : pageRs);

        var writer = mock(ActCiRelationWriter.class);
        when(writer.write(anyList())).thenReturn(1);

        new CiRelationJob(factory, writer).run();

        /*
         * openConnection() 6회: 소스1 실패(1) + 소스2 DISK 건수·페이지(2)
         * + 소스3 FILESYSTEM 건수(1) + 소스4 HOST_VIRTUALIZES_VM 건수(1)
         * + 소스5 DB_INSTANCE_RUNS_ON_DEVICE 건수(1).
         * atLeast(values().length)이던 이전 단언은 "루프가 소스2 직후 멈춘 회귀"도
         * 통과시켰다(그 경우도 3회는 채워진다). 정확한 횟수로 조인다.
         */
        verify(factory, times(6)).openConnection();
        verify(writer, atLeastOnce()).write(anyList());

        /*
         * 호출 횟수만으로는 "세 번째 소스에 도달했다"를 증명하지 못한다 — FILESYSTEM의
         * countRs.next()가 DISK의 건수 호출로 이미 소진돼 0건으로 조용히 스킵되는 것과
         * "루프가 소스3에 아예 도달하지 못한 것"을 구분할 수 없기 때문이다. 실행된 SQL을
         * 직접 캡처해 FILESYSTEM 고유 테이블(view_mountpoint_v2)이 조회됐는지 확인한다.
         */
        var executedQueries = ArgumentCaptor.forClass(String.class);
        verify(statement, times(5)).executeQuery(executedQueries.capture());
        assertThat(executedQueries.getAllValues())
                .as("세 번째 소스(FILESYSTEM)의 건수 쿼리가 실제로 실행됐다")
                .anyMatch(sql -> sql.contains("view_mountpoint_v2"));
        assertThat(executedQueries.getAllValues())
                .as("네 번째 소스(HOST_VIRTUALIZES_VM)의 건수 쿼리가 실제로 실행됐다")
                .anyMatch(sql -> sql.contains("virtual_host_device_fk"));
        assertThat(executedQueries.getAllValues())
                .as("다섯 번째 소스(DB_INSTANCE_RUNS_ON_DEVICE)의 건수 쿼리가 실제로 실행됐다")
                .anyMatch(sql -> sql.contains("view_databaseinstance_v2"));
    }

    /**
     * 건수 조회는 count를, 페이지 조회는 관계 한 쌍을 돌려준다.
     * ResultSet 목은 Answer 밖에서 미리 만든다. Answer 안에서 when()을 부르면
     * Mockito의 진행 중 스터빙 상태가 꼬인다.
     */
    private static void stubCount(Device42ConnectionFactory factory, long count) throws SQLException {
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        var countRs = mock(ResultSet.class);
        when(countRs.next()).thenReturn(true, false);
        when(countRs.getLong(1)).thenReturn(count);

        var pageRs = mock(ResultSet.class);
        when(pageRs.next()).thenReturn(true, false);
        when(pageRs.getString("sourceci")).thenReturn("D42:DEVICEOS:147");
        when(pageRs.getString("targetci")).thenReturn("D42:DEVICE:173");

        when(statement.executeQuery(anyString())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).contains("COUNT(*)") ? countRs : pageRs);
    }
}
