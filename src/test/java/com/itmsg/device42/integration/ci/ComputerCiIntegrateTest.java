package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.ComputerSource;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ComputerCiIntegrateTest {
    private JdbcTemplate jdbc;
    private ComputerCiIntegrate integration;
    private MockEnvironment environment;
    private final CiLoadSettings settings = new CiLoadSettings("ETL", "KO", ZoneId.of("Asia/Seoul"), 2);
    private static final List<String> TEXT = List.of(
            "NAME", "SERIALNUMBER", "UUID", "MANUFACTURER", "MODEL", "CPUTYPE", "ARCHITECTURE",
            "PRIMARYMACADDRESS", "TYPE", "VIRTUAL", "VMID", "BIOSMANUFACTURER", "ROMVERSION", "BIOSRELEASEDATE");
    private static final List<String> NUMBER = List.of("MEMORYSIZE", "NUMCPUS", "CPUSPEED", "CPUCORESINSTALLED");

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        environment = new MockEnvironment().withProperty("ci.change-by", "ETL")
                .withProperty("ci.lang-code", "KO").withProperty("ci.zone-id", "Asia/Seoul")
                .withProperty("ci.page-size", "2");
        integration = new ComputerCiIntegrate(mock(Device42ConnectionFactory.class), jdbc, environment);
        seedDefinitions();
    }

    private void seedDefinitions() {
        jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES ('PHYS','SYS.COMPUTERSYSTEM'),('VM','SYS.VIRTUALCOMPUTERSYSTEM')");
        jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES ('PHYS','ACTCI'),('VM','ACTCI')");
        jdbc.update("INSERT INTO MAXIMO.MEASUREUNIT VALUES ('GBYTE'),('MBYTE'),('GHZ'),('MHZ')");
        List<String> attributes = new ArrayList<>(TEXT);
        attributes.addAll(NUMBER);
        long templateId = 100;
        for (String classId : List.of("PHYS", "VM")) {
            for (String suffix : attributes) {
                String attribute = "COMPUTERSYSTEM_" + suffix;
                long id = templateId++;
                jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE VALUES (?,?,?)",
                        id, attribute, NUMBER.contains(suffix) ? "NUMERIC" : "ALN");
                jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES (?,?,?,?)",
                        classId, id, attribute, id);
                jdbc.update("""
                        INSERT INTO MAXIMO.CLASSSPECUSEWITH
                        (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                        VALUES (?,'ACTCI',?,0,1,?,?)
                        """, id, id, classId, attribute);
            }
        }
    }

    private ComputerSource source(long id, String type, String name, BigDecimal ram) {
        return new ComputerSource(id, type, name, "description", "serial", "uuid",
                "2026-09-14 00:00:00.123456+00", "model", "manufacturer", ram, "GB",
                2, 12, new BigDecimal("2.40"), "GHz", "Xeon", "x86_64",
                "001122334455", "vm-internal-id", "BIOS vendor", "1.2", "11/12/2021");
    }

    @Test
    void mapsPhysicalAndVirtualUsingTheirOwnTemplatesAndPreservesUnits() {
        var definitions = integration.loadDefinitions();
        integration.saveComputer(source(7, "physical", "Physical", new BigDecimal("32.125")), definitions, settings);
        integration.saveComputer(source(8, "virtual", "Virtual", new BigDecimal("16")), definitions, settings);

        assertThat(jdbc.queryForList("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI ORDER BY ACTCINUM", String.class))
                .containsExactly("PHYS", "VM");
        assertThat(value("D42:DEVICE:7", "CPUCORESINSTALLED", "NUMVALUE"))
                .isEqualByComparingTo("24");
        assertThat(value("D42:DEVICE:7", "MEMORYSIZE", "NUMVALUE")).isEqualByComparingTo("32.125");
        assertThat(text("D42:DEVICE:7", "MEMORYSIZE", "MEASUREUNITID")).isEqualTo("GBYTE");
        assertThat(text("D42:DEVICE:7", "CPUSPEED", "MEASUREUNITID")).isEqualTo("GHZ");
        assertThat(text("D42:DEVICE:7", "VIRTUAL", "ALNVALUE")).isEqualTo("false");
        assertThat(text("D42:DEVICE:8", "VIRTUAL", "ALNVALUE")).isEqualTo("true");
        assertThat(text("D42:DEVICE:8", "VMID", "ALNVALUE")).isEqualTo("vm-internal-id");
        assertThat(text("D42:DEVICE:7", "BIOSRELEASEDATE", "ALNVALUE")).isEqualTo("11/12/2021");
        assertThat(jdbc.queryForObject("SELECT LASTSCANDT FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:DEVICE:7'",
                LocalDateTime.class)).isEqualTo(LocalDateTime.of(2026, 9, 14, 9, 0, 0, 123456000));
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM MAXIMO.ACTCISPEC s
                JOIN MAXIMO.ACTCI c ON c.ACTCIID=s.REFOBJECTID AND c.ACTCINUM=s.ACTCINUM
                JOIN MAXIMO.CLASSSPEC t ON t.CLASSSPECID=s.CLASSSPECID
                  AND t.CLASSSTRUCTUREID=c.CLASSSTRUCTUREID AND t.ASSETATTRID=s.ASSETATTRID
                WHERE s.REFOBJECTNAME='ACTCI' AND s.CLASSSTRUCTUREID=c.CLASSSTRUCTUREID
                """, Integer.class)).isEqualTo(count("ACTCISPEC"));
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM MAXIMO.ACTCISPEC WHERE
                    (ALNVALUE IS NOT NULL AND NUMVALUE IS NOT NULL) OR TABLEVALUE IS NOT NULL
                """, Integer.class)).isZero();
    }

    @Test
    void rerunKeepsParentAndSpecIdsIncludingNullSections() {
        var definitions = integration.loadDefinitions();
        integration.saveComputer(source(7, "physical", "Before", new BigDecimal("32")), definitions, settings);
        long parentId = jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class);
        var specIds = jdbc.queryForList("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC ORDER BY ACTCISPECID", Long.class);
        integration.saveComputer(source(7, "physical", "After", new BigDecimal("64")), definitions, settings);

        assertThat(count("ACTCI")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class)).isEqualTo(parentId);
        assertThat(jdbc.queryForList("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC ORDER BY ACTCISPECID", Long.class))
                .isEqualTo(specIds);
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("After");
        assertThat(value("D42:DEVICE:7", "MEMORYSIZE", "NUMVALUE")).isEqualByComparingTo("64");
        assertThat(parentId).isNotEqualTo(7);
    }

    @Test
    void failedSpecInsertRollsBackParentAndEarlierSpecs() {
        var definitions = integration.loadDefinitions();
        rejectBiosVersion();
        assertThatThrownBy(() -> integration.saveComputer(
                source(7, "physical", "Fail", BigDecimal.TEN), definitions, settings)).isInstanceOf(RuntimeException.class);
        assertThat(count("ACTCI")).isZero();
        assertThat(count("ACTCISPEC")).isZero();
    }

    @Test
    void failedSpecUpdateRollsBackParentAndEarlierSpecChanges() {
        var definitions = integration.loadDefinitions();
        integration.saveComputer(source(7, "physical", "Before", BigDecimal.TEN), definitions, settings);
        jdbc.update("ALTER TABLE MAXIMO.ACTCISPEC ADD CONSTRAINT reject_new_name CHECK (ALNVALUE <> 'After')");
        assertThatThrownBy(() -> integration.saveComputer(
                source(7, "physical", "After", BigDecimal.ONE), definitions, settings)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("Before");
        assertThat(text("D42:DEVICE:7", "NAME", "ALNVALUE")).isEqualTo("Before");
    }

    @Test
    void missingNewAttributeFailsBeforeSourceReadsOrWrites() {
        jdbc.update("DELETE FROM MAXIMO.CLASSSPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        var reads = new AtomicInteger();
        var task = new ComputerCiIntegrate(mock(Device42ConnectionFactory.class), jdbc, environment) {
            @Override public List<ComputerSource> getData(long cursor, int limit) {
                reads.incrementAndGet();
                return List.of();
            }
        };
        assertThatThrownBy(task::integrate).hasMessageContaining("BIOSRELEASEDATE");
        assertThat(reads).hasValue(0);
        assertThat(count("ACTCI")).isZero();
    }

    @Test
    void rejectsWrongDataTypeDuplicateTemplatesAndWrongUseWithReference() {
        jdbc.update("UPDATE MAXIMO.ASSETATTRIBUTE SET DATATYPE='ALN' WHERE ASSETATTRID='COMPUTERSYSTEM_MEMORYSIZE'");
        assertThatThrownBy(integration::loadDefinitions).hasMessageContaining("MEMORYSIZE");
        jdbc.update("UPDATE MAXIMO.ASSETATTRIBUTE SET DATATYPE='NUMERIC' WHERE ASSETATTRID='COMPUTERSYSTEM_MEMORYSIZE'");
        jdbc.update("UPDATE MAXIMO.CLASSSPECUSEWITH SET CLASSSTRUCTUREID='WRONG' WHERE ASSETATTRID='COMPUTERSYSTEM_NAME'");
        assertThatThrownBy(integration::loadDefinitions).hasMessageContaining("COMPUTERSYSTEM_NAME");
        jdbc.update("UPDATE MAXIMO.CLASSSPECUSEWITH SET CLASSSTRUCTUREID='PHYS' WHERE ASSETATTRID='COMPUTERSYSTEM_NAME' AND CLASSSPECID=100");
        jdbc.update("UPDATE MAXIMO.CLASSSPECUSEWITH SET CLASSSTRUCTUREID='VM' WHERE ASSETATTRID='COMPUTERSYSTEM_NAME' AND CLASSSPECID<>100");
        jdbc.update("INSERT INTO MAXIMO.CLASSSPECUSEWITH SELECT * FROM MAXIMO.CLASSSPECUSEWITH WHERE CLASSSPECID=100");
        assertThatThrownBy(integration::loadDefinitions).hasMessageContaining("중복");
    }

    @Test
    void readsPagesByLastSourceKeyAndLoadsDefinitionsOncePerRun() {
        List<Long> cursors = new ArrayList<>();
        var definitionReads = new AtomicInteger();
        var task = new ComputerCiIntegrate(mock(Device42ConnectionFactory.class), jdbc, environment) {
            @Override Map<String, ClassificationDefinition> loadDefinitions() {
                definitionReads.incrementAndGet();
                return super.loadDefinitions();
            }
            @Override public List<ComputerSource> getData(long cursor, int limit) {
                cursors.add(cursor);
                assertThat(limit).isEqualTo(2);
                return switch ((int) cursor) {
                    case 0 -> List.of(source(7, "physical", "A", BigDecimal.ONE), source(20, "virtual", "B", BigDecimal.TEN));
                    case 20 -> List.of(source(45, "virtual", "C", BigDecimal.ONE));
                    default -> List.of();
                };
            }
        };
        task.integrate();
        assertThat(cursors).containsExactly(0L, 20L, 45L);
        assertThat(definitionReads).hasValue(1);
        assertThat(count("ACTCI")).isEqualTo(3);
    }

    @Test
    void missingSpecValueDoesNotInventZeroOrClearExistingValue() {
        var definitions = integration.loadDefinitions();
        integration.saveComputer(source(7, "physical", "Host", BigDecimal.TEN), definitions, settings);
        integration.saveComputer(source(7, "physical", "Host", null), definitions, settings);
        assertThat(value("D42:DEVICE:7", "MEMORYSIZE", "NUMVALUE")).isEqualByComparingTo("10");
        integration.saveComputer(source(8, "virtual", "Empty", null), definitions, settings);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM MAXIMO.ACTCISPEC WHERE ACTCINUM='D42:DEVICE:8'
                  AND ASSETATTRID='COMPUTERSYSTEM_MEMORYSIZE'
                """, Integer.class)).isZero();
    }

    @Test
    void refusesUnresolvedClassChangesWithoutChangingExistingRows() {
        var definitions = integration.loadDefinitions();
        integration.saveComputer(source(7, "physical", "Physical", BigDecimal.TEN), definitions, settings);
        assertThatThrownBy(() -> integration.saveComputer(
                source(7, "virtual", "Virtual", BigDecimal.ONE), definitions, settings)).hasMessageContaining("분류 변경");
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class)).isEqualTo("PHYS");
    }

    @Test
    void missingSettingsFailOnlyWhenCiIsRun() {
        var task = new ComputerCiIntegrate(mock(Device42ConnectionFactory.class), jdbc, new MockEnvironment());
        assertThatThrownBy(task::integrate).hasMessageContaining("ci.change-by");
        assertThat(count("ACTCI")).isZero();
    }

    private void rejectBiosVersion() {
        jdbc.update("ALTER TABLE MAXIMO.ACTCISPEC ADD CONSTRAINT reject_bios CHECK (ASSETATTRID <> 'COMPUTERSYSTEM_ROMVERSION')");
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO." + table, Integer.class);
    }

    private String text(String ci, String suffix, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM MAXIMO.ACTCISPEC WHERE ACTCINUM=? AND ASSETATTRID=?",
                String.class, ci, "COMPUTERSYSTEM_" + suffix);
    }

    private BigDecimal value(String ci, String suffix, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM MAXIMO.ACTCISPEC WHERE ACTCINUM=? AND ASSETATTRID=?",
                BigDecimal.class, ci, "COMPUTERSYSTEM_" + suffix);
    }
}
