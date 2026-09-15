package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
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
