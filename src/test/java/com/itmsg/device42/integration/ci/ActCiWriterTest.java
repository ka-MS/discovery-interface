package com.itmsg.device42.integration.ci;

import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActCiWriterTest {
    private static final LocalDateTime SCANNED = LocalDateTime.of(2026, 9, 15, 1, 0);
    private static final LocalDateTime CHANGED = LocalDateTime.of(2026, 9, 15, 2, 0);

    private JdbcTemplate jdbc;
    private ActCiWriter writer;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        writer = new ActCiWriter(jdbc);
    }

    @Test
    void savesParentAndSpecsAndCountsLoadedParents() {
        int loaded = writer.write(List.of(ci("D42:OS:1", "RHEL", "OSCLASS", spec("D42:OS:1", "OSCLASS", "OS_NAME", "RHEL"))));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:OS:1'", String.class))
                .isEqualTo("RHEL");
        assertThat(jdbc.queryForObject("""
                SELECT s.ALNVALUE FROM MAXIMO.ACTCISPEC s
                JOIN MAXIMO.ACTCI c ON c.ACTCIID = s.REFOBJECTID AND c.ACTCINUM = s.ACTCINUM
                WHERE s.ASSETATTRID='OS_NAME'
                """, String.class)).isEqualTo("RHEL");
    }

    @Test
    void rerunKeepsGeneratedIdsAndUpdatesValues() {
        writer.write(List.of(ci("D42:OS:1", "Before", "OSCLASS", spec("D42:OS:1", "OSCLASS", "OS_NAME", "Before"))));
        long parentId = jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class);
        long specId = jdbc.queryForObject("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC", Long.class);

        writer.write(List.of(ci("D42:OS:1", "After", "OSCLASS", spec("D42:OS:1", "OSCLASS", "OS_NAME", "After"))));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCI", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class)).isEqualTo(parentId);
        assertThat(jdbc.queryForObject("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC", Long.class)).isEqualTo(specId);
        assertThat(jdbc.queryForObject("SELECT ALNVALUE FROM MAXIMO.ACTCISPEC", String.class)).isEqualTo("After");
    }

    @Test
    void updatesClassificationAndSpecsWithoutChangingGeneratedIds() {
        writer.write(List.of(ci("D42:OS:1", "RHEL", "OSCLASS", spec("D42:OS:1", "OSCLASS", "OS_NAME", "RHEL"))));
        long parentId = jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class);
        long specId = jdbc.queryForObject("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC", Long.class);

        int loaded = writer.write(List.of(ci("D42:OS:1", "Windows", "OTHERCLASS",
                spec("D42:OS:1", "OTHERCLASS", "OS_NAME", 200L, "Windows"))));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class)).isEqualTo(parentId);
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class)).isEqualTo("OTHERCLASS");
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("Windows");
        assertThat(jdbc.queryForObject("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC", Long.class)).isEqualTo(specId);
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCISPEC", String.class)).isEqualTo("OTHERCLASS");
        assertThat(jdbc.queryForObject("SELECT CLASSSPECID FROM MAXIMO.ACTCISPEC", Long.class)).isEqualTo(200L);
        assertThat(jdbc.queryForObject("SELECT ALNVALUE FROM MAXIMO.ACTCISPEC", String.class)).isEqualTo("Windows");
    }

    @Test
    void failedParentCreatesNoSpecsAndDoesNotStopFollowingCi() {
        int loaded = writer.write(List.of(
                ci("D42:OS:1", "X".repeat(193), "OSCLASS", spec("D42:OS:1", "OSCLASS", "OS_NAME", "TooLong")),
                ci("D42:OS:2", "Good", "OSCLASS", spec("D42:OS:2", "OSCLASS", "OS_NAME", "Good"))));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class))
                .containsExactly("D42:OS:2");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM MAXIMO.ACTCISPEC WHERE ACTCINUM='D42:OS:1'", Integer.class)).isZero();
    }

    @Test
    void failedSpecKeepsParentAndContinuesRemainingSpecs() {
        jdbc.update("ALTER TABLE MAXIMO.ACTCISPEC ADD CONSTRAINT reject_bad CHECK (ALNVALUE <> 'BAD')");

        int loaded = writer.write(List.of(ci("D42:OS:1", "RHEL", "OSCLASS",
                spec("D42:OS:1", "OSCLASS", "OS_BAD", "BAD"),
                spec("D42:OS:1", "OSCLASS", "OS_NAME", "RHEL"))));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCI", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC", String.class))
                .containsExactly("OS_NAME");
    }

    private static CiUpsert ci(String actCiNum, String name, String classId, ActCiSpecUpsert... specs) {
        return new CiUpsert(
                new ActCiUpsert(actCiNum, name, classId, "설명", SCANNED, "Device42", CHANGED, "KO"),
                List.of(specs));
    }

    private static ActCiSpecUpsert spec(String actCiNum, String classId, String attributeId, String value) {
        return spec(actCiNum, classId, attributeId, 100L, value);
    }

    private static ActCiSpecUpsert spec(String actCiNum, String classId, String attributeId,
                                        long classSpecId, String value) {
        return new ActCiSpecUpsert(actCiNum, classId, attributeId, classSpecId, null, 10, false,
                null, null, null, value, (BigDecimal) null, "Device42", CHANGED);
    }
}
