package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
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
    void everyPageQueryOrdersByRelationKeyAndPages() {
        for (CiRelationSource source : CiRelationSource.values()) {
            assertThat(source.pageQuery(20, 10))
                    .as("%s 페이지 SQL", source)
                    .contains("AS sourceci")
                    .contains("AS targetci")
                    .contains("ORDER BY sourceci, targetci")
                    .contains("LIMIT 10 OFFSET 20");
            assertThat(source.countQuery()).as("%s 건수 SQL", source).contains("COUNT(*)");
            assertThat(source.relationNum()).as("%s 관계 코드", source).startsWith("RELATION.");
        }
    }

    @Test
    void readsEveryPageAndReportsLoadedCount() throws Exception {
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
