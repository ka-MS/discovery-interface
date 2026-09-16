package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.OsSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OsCiIntegrateTest {
    private static final List<String> ATTRIBUTES = List.of(
            "OSNAME", "OSVERSION", "KERNELVERSION", "KERNELARCHITECTURE", "NAME");

    private JdbcTemplate jdbc;
    private OsCiIntegrate integration;
    private CiDefinitionLoader definitionLoader;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        definitionLoader = new CiDefinitionLoader(jdbc);
        integration = new OsCiIntegrate(mock(Device42ConnectionFactory.class), new ActCiWriter(jdbc), new CiSpecMapper());
        seedDefinitions();
    }

    private void seedDefinitions() {
        jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES ('OSC','SYS.OPERATINGSYSTEM')");
        jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES ('OSC','ACTCI')");
        long id = 500;
        for (String suffix : ATTRIBUTES) {
            String attribute = "OPERATINGSYSTEM_" + suffix;
            jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,'ALN')",
                    id, attribute);
            jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES ('OSC',?,?,?)",
                    id, attribute, id);
            jdbc.update("""
                    INSERT INTO MAXIMO.CLASSSPECUSEWITH
                        (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                    VALUES (?,'ACTCI',?,0,1,'OSC',?)
                    """, id, id, attribute);
            id++;
        }
    }

    @Test
    void mapsBodyAndFiveSpecsUsingParentScanTime() {
        persist(source(7, 100, "IBM Red Hat Enterprise Linux 8.10"));

        assertThat(jdbc.queryForObject("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class)).isEqualTo("D42:DEVICEOS:7");
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class))
                .isEqualTo("IBM Red Hat Enterprise Linux 8.10");
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class)).isEqualTo("OSC");
        assertThat(jdbc.queryForObject("SELECT LASTSCANDT FROM MAXIMO.ACTCI", java.time.LocalDateTime.class))
                .isEqualTo(OffsetDateTime.parse("2026-09-15T00:00:00Z")
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC ORDER BY ASSETATTRID", String.class))
                .containsExactly("OPERATINGSYSTEM_KERNELARCHITECTURE", "OPERATINGSYSTEM_KERNELVERSION",
                        "OPERATINGSYSTEM_NAME", "OPERATINGSYSTEM_OSNAME", "OPERATINGSYSTEM_OSVERSION");
        assertThat(text("OPERATINGSYSTEM_NAME")).isEqualTo("IBM Red Hat Enterprise Linux 8.10");
        assertThat(text("OPERATINGSYSTEM_OSNAME")).isEqualTo("IBM Red Hat Enterprise Linux 8.10");
        assertThat(text("OPERATINGSYSTEM_KERNELVERSION")).isEqualTo("4.18.0-513.5.1.el8_9.x86_64");
    }

    @Test
    void missingValuesCreateTemplateRowsWithNullValues() {
        var row = new OsSource(8, 101, "Alpine Linux 3.21", null, null, null, "2026-09-15T00:00:00Z");
        persist(row);

        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC ORDER BY ASSETATTRID", String.class))
                .containsExactly("OPERATINGSYSTEM_KERNELARCHITECTURE", "OPERATINGSYSTEM_KERNELVERSION",
                        "OPERATINGSYSTEM_NAME", "OPERATINGSYSTEM_OSNAME", "OPERATINGSYSTEM_OSVERSION");
        assertThat(text("OPERATINGSYSTEM_OSVERSION")).isNull();
        assertThat(text("OPERATINGSYSTEM_KERNELVERSION")).isNull();
        assertThat(text("OPERATINGSYSTEM_KERNELARCHITECTURE")).isNull();
    }

    @Test
    void missingClassificationSkipsEveryOs() {
        jdbc.update("DELETE FROM MAXIMO.CLASSUSEWITH WHERE CLASSSTRUCTUREID='OSC'");

        var mapped = integration.mapData(List.of(source(7, 100, "RHEL")), definitionLoader.load());

        assertThat(mapped).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCI", Integer.class)).isZero();
    }

    @Test
    void unparsableScanTimeSkipsThatOsOnly() {
        var bad = new OsSource(7, 100, "RHEL", "8.10", "4.18", "64-bit", "not-a-time");
        var good = source(8, 101, "Ubuntu 24.04");

        var mapped = integration.mapData(List.of(bad, good), definitionLoader.load());

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().actCi().actCiNum()).isEqualTo("D42:DEVICEOS:8");
    }

    @Test
    void readsEveryOffsetUntilTotalCount() {
        List<Long> offsets = new ArrayList<>();
        var task = new OsCiIntegrate(mock(Device42ConnectionFactory.class), new ActCiWriter(jdbc), new CiSpecMapper()) {
            @Override public long getTotalCount() {
                return 2001;
            }
            @Override public List<OsSource> getData(long offset, int limit) {
                offsets.add(offset);
                assertThat(limit).isEqualTo(offset == 2000 ? 1 : 1000);
                return List.of(source(offset + 1, 100 + offset, "RHEL"));
            }
        };

        task.integrate(definitionLoader.load());

        assertThat(offsets).containsExactly(0L, 1000L, 2000L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCI", Integer.class)).isEqualTo(3);
    }

    private static OsSource source(long osPk, long devicePk, String name) {
        return new OsSource(osPk, devicePk, name, "8.10", "4.18.0-513.5.1.el8_9.x86_64", "64-bit",
                "2026-09-15T00:00:00Z");
    }

    private void persist(OsSource source) {
        integration.putData(integration.mapData(List.of(source), definitionLoader.load()));
    }

    private String text(String attributeId) {
        return jdbc.queryForObject("SELECT ALNVALUE FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID=?", String.class, attributeId);
    }
}
