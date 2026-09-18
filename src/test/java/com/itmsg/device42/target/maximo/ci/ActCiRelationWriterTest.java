package com.itmsg.device42.target.maximo.ci;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActCiRelationWriterTest {
    private static final String INSTALLED_ON = "RELATION.INSTALLEDON";

    private JdbcTemplate jdbc;
    private ActCiRelationWriter writer;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        writer = new ActCiRelationWriter(jdbc);

        jdbc.update("INSERT INTO MAXIMO.RELATION VALUES (?)", INSTALLED_ON);
        actCi("D42:DEVICEOS:147", "OS1");
        actCi("D42:DEVICEOS:148", "OS1");
        actCi("D42:DEVICE:173", "CS1");
        actCi("D42:DEVICE:174", "VCS1");
        rule(INSTALLED_ON, "OS1", "CS1");
        rule(INSTALLED_ON, "OS1", "VCS1");
    }

    private void actCi(String actCiNum, String classStructureId) {
        jdbc.update("""
                INSERT INTO MAXIMO.ACTCI
                    (ACTCIID,ACTCINUM,ACTCINAME,CLASSSTRUCTUREID,LASTSCANDT,LANGCODE,HASLD)
                VALUES (NEXT VALUE FOR MAXIMO.ACTCISEQ,?,?,?,CURRENT_TIMESTAMP,'KO',0)
                """, actCiNum, actCiNum, classStructureId);
    }

    private void rule(String relationNum, String sourceClass, String targetClass) {
        jdbc.update("INSERT INTO MAXIMO.RELATIONRULES VALUES (?,?,?)",
                relationNum, sourceClass, targetClass);
    }

    private static ActCiRelationUpsert relation(String source, String target) {
        return new ActCiRelationUpsert(source, target, INSTALLED_ON);
    }

    @Test
    void insertsRelationWithConstantSwappedAndChangeBy() {
        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SWAPPED FROM MAXIMO.ACTCIRELATION", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT CHANGEBY FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("Device42");
        assertThat(jdbc.queryForObject("SELECT CHANGEDATE FROM MAXIMO.ACTCIRELATION", Object.class)).isNotNull();
    }

    @Test
    void rerunKeepsSingleRowAndSameId() {
        writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));
        Long firstId = jdbc.queryForObject("SELECT ACTCIRELATIONID FROM MAXIMO.ACTCIRELATION", Long.class);

        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCIRELATION", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCIRELATIONID FROM MAXIMO.ACTCIRELATION", Long.class))
                .isEqualTo(firstId);
    }

    @Test
    void acceptsPhysicalAndVirtualComputerWithoutCallerBranching() {
        int loaded = writer.write(List.of(
                relation("D42:DEVICEOS:147", "D42:DEVICE:173"),
                relation("D42:DEVICEOS:148", "D42:DEVICE:174")));

        assertThat(loaded).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT TARGETCI FROM MAXIMO.ACTCIRELATION ORDER BY TARGETCI", String.class))
                .containsExactly("D42:DEVICE:173", "D42:DEVICE:174");
    }

    @Test
    void skipsMissingEndAndKeepsProcessingRest() {
        int loaded = writer.write(List.of(
                relation("D42:DEVICEOS:999", "D42:DEVICE:173"),
                relation("D42:DEVICEOS:147", "D42:DEVICE:999"),
                relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SOURCECI FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("D42:DEVICEOS:147");
    }

    @Test
    void skipsWhenNoRuleForActualClassPair() {
        jdbc.update("DELETE FROM MAXIMO.RELATIONRULES WHERE TARGETCLASS='CS1'");

        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCIRELATION", Integer.class)).isZero();
    }

    @Test
    void continuesAfterDatabaseRejectsFirstRelation() {
        // 양 끝과 규칙이 정상인 첫 관계만 CHECK 제약으로 거부해 실제 저장 예외를 발생시킨다.
        jdbc.execute("""
                ALTER TABLE MAXIMO.ACTCIRELATION ADD CONSTRAINT REJECT_FIRST_RELATION
                CHECK (SOURCECI <> 'D42:DEVICEOS:147')
                """);

        int loaded = writer.write(List.of(
                relation("D42:DEVICEOS:147", "D42:DEVICE:173"),
                relation("D42:DEVICEOS:148", "D42:DEVICE:174")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT SOURCECI FROM MAXIMO.ACTCIRELATION", String.class))
                .containsExactly("D42:DEVICEOS:148");
    }

    @Test
    void keepsBothComputerLinksToOneFilesystemAndTheirIdsOnRerun() {
        String contains = "RELATION.CONTAINS";
        jdbc.update("INSERT INTO MAXIMO.RELATION VALUES (?)", contains);
        actCi("D42:MOUNTPOINT:10", "FS1");
        rule(contains, "CS1", "FS1");
        rule(contains, "VCS1", "FS1");
        var physical = new ActCiRelationUpsert("D42:DEVICE:173", "D42:MOUNTPOINT:10", contains);
        var virtual = new ActCiRelationUpsert("D42:DEVICE:174", "D42:MOUNTPOINT:10", contains);

        assertThat(writer.write(List.of(physical, virtual))).isEqualTo(2);
        String snapshotQuery = """
                SELECT SOURCECI, TARGETCI, RELATIONNUM, ACTCIRELATIONID
                FROM MAXIMO.ACTCIRELATION ORDER BY SOURCECI
                """;
        var firstRows = jdbc.queryForList(snapshotQuery);
        assertThat(firstRows).hasSize(2);
        assertThat(firstRows).extracting(row -> row.get("SOURCECI"))
                .containsExactly(physical.sourceCiNum(), virtual.sourceCiNum());
        assertThat(firstRows).allSatisfy(row -> {
            assertThat(row.get("TARGETCI")).isEqualTo("D42:MOUNTPOINT:10");
            assertThat(row.get("RELATIONNUM")).isEqualTo(contains);
        });

        assertThat(writer.write(List.of(virtual, physical))).isEqualTo(2);
        assertThat(jdbc.queryForList(snapshotQuery)).isEqualTo(firstRows);
    }

    @Test
    void storesPhysicalAndVirtualHostsAsSourceOfVirtualizes() {
        String virtualizes = "VIRTUALIZES";
        jdbc.update("INSERT INTO MAXIMO.RELATION VALUES (?)", virtualizes);
        actCi("D42:DEVICE:175", "VCS1");
        actCi("D42:DEVICE:176", "VCS1");
        actCi("D42:DEVICE:177", "VCS1");
        rule(virtualizes, "CS1", "VCS1");
        rule(virtualizes, "VCS1", "VCS1");

        var physicalHostFirstVm = new ActCiRelationUpsert(
                "D42:DEVICE:173", "D42:DEVICE:175", virtualizes);
        var physicalHostSecondVm = new ActCiRelationUpsert(
                "D42:DEVICE:173", "D42:DEVICE:176", virtualizes);
        var virtualHost = new ActCiRelationUpsert(
                "D42:DEVICE:174", "D42:DEVICE:177", virtualizes);

        assertThat(writer.write(List.of(physicalHostFirstVm, physicalHostSecondVm, virtualHost))).isEqualTo(3);
        assertThat(jdbc.queryForList(
                "SELECT SOURCECI FROM MAXIMO.ACTCIRELATION ORDER BY SOURCECI", String.class))
                .containsExactly("D42:DEVICE:173", "D42:DEVICE:173", "D42:DEVICE:174");
        assertThat(jdbc.queryForList(
                "SELECT TARGETCI FROM MAXIMO.ACTCIRELATION ORDER BY SOURCECI", String.class))
                .containsExactlyInAnyOrder("D42:DEVICE:175", "D42:DEVICE:176", "D42:DEVICE:177");
        assertThat(jdbc.queryForList(
                "SELECT SWAPPED FROM MAXIMO.ACTCIRELATION", Integer.class))
                .containsOnly(0);
    }

    @Test
    void skipsWhenRelationCodeIsNotRegistered() {
        jdbc.update("DELETE FROM MAXIMO.RELATION");

        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isZero();
    }

    @Test
    void keepsExistingGuidAndAncestorOnUpdate() {
        writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));
        jdbc.update("UPDATE MAXIMO.ACTCIRELATION SET SOURCECIGUID='urn:uuid:kept', ANCESTORCI='D42:DEVICE:173'");

        writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(jdbc.queryForObject("SELECT SOURCECIGUID FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("urn:uuid:kept");
        assertThat(jdbc.queryForObject("SELECT ANCESTORCI FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("D42:DEVICE:173");
    }
}
