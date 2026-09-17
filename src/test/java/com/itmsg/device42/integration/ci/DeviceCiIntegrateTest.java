package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.DeviceSource;
import com.itmsg.device42.enums.ci.CiClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceCiIntegrateTest {
    private static final String COMPUTER_CLASS_STRUCTURE_ID = "PHYS";
    private static final String VIRTUAL_CLASS_STRUCTURE_ID =
            CiClassification.COMPUTER.classificationId()
                    .equals(CiClassification.VIRTUAL_COMPUTER.classificationId())
                    ? COMPUTER_CLASS_STRUCTURE_ID : "VM";
    private static final String SWITCH_CLASS_STRUCTURE_ID = "SWITCH";
    private static final String CLUSTER_CLASS_STRUCTURE_ID = "CLUSTER";
    private JdbcTemplate jdbc;
    private DeviceCiIntegrate integration;
    private CiDefinitionLoader definitionLoader;
    private static final List<String> TEXT = List.of(
            "NAME", "SERIALNUMBER", "UUID", "MANUFACTURER", "MODEL", "CPUTYPE", "ARCHITECTURE",
            "PRIMARYMACADDRESS", "TYPE", "VIRTUAL", "VMID", "BIOSMANUFACTURER", "ROMVERSION", "BIOSRELEASEDATE");
    private static final List<String> NUMBER = List.of("MEMORYSIZE", "NUMCPUS", "CPUSPEED", "CPUCORESINSTALLED");

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = spy(new JdbcTemplate(dataSource));
        definitionLoader = new CiDefinitionLoader(jdbc);
        integration = new DeviceCiIntegrate(mock(Device42ConnectionFactory.class), new ActCiWriter(jdbc),
                new CiSpecMapper());
        seedDefinitions();
    }

    private void seedDefinitions() {
        Map<String, String> classStructures = new LinkedHashMap<>();
        classStructures.put(CiClassification.COMPUTER.classificationId(), COMPUTER_CLASS_STRUCTURE_ID);
        classStructures.putIfAbsent(CiClassification.VIRTUAL_COMPUTER.classificationId(), VIRTUAL_CLASS_STRUCTURE_ID);
        classStructures.putIfAbsent(CiClassification.GENERIC_SWITCH.classificationId(), SWITCH_CLASS_STRUCTURE_ID);
        classStructures.putIfAbsent(
                CiClassification.COMPUTER_SYSTEM_CLUSTER.classificationId(), CLUSTER_CLASS_STRUCTURE_ID);
        classStructures.forEach((classificationId, classStructureId) -> {
            jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES (?,?)", classStructureId, classificationId);
            jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES (?,'ACTCI')", classStructureId);
        });
        jdbc.update("INSERT INTO MAXIMO.MEASUREUNIT VALUES ('GBYTE'),('MBYTE'),('GHZ'),('MHZ')");
        List<String> attributes = new ArrayList<>(TEXT);
        attributes.addAll(NUMBER);
        long templateId = 100;
        for (String suffix : attributes) {
            String attribute = "COMPUTERSYSTEM_" + suffix;
            long attributeId = 100 + attributes.indexOf(suffix);
            jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,?)",
                    attributeId, attribute, NUMBER.contains(suffix) ? "NUMERIC" : "ALN");
        }
        for (String classId : classStructures.values()) {
            if (CLUSTER_CLASS_STRUCTURE_ID.equals(classId)) {
                continue;
            }
            for (String suffix : attributes) {
                String attribute = "COMPUTERSYSTEM_" + suffix;
                long id = templateId++;
                long attributeId = 100 + attributes.indexOf(suffix);
                jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES (?,?,?,?)",
                        classId, id, attribute, attributeId);
                jdbc.update("""
                        INSERT INTO MAXIMO.CLASSSPECUSEWITH
                        (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                        VALUES (?,'ACTCI',?,0,1,?,?)
                        """, id, id, classId, attribute);
            }
        }
        jdbc.update("""
                INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE)
                VALUES (1900,'GENERICCOMPUTERSYSTEM_GENERICTYPE','ALN')
                """);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPEC
                    (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID)
                VALUES (?,1900,'GENERICCOMPUTERSYSTEM_GENERICTYPE',1900)
                """, SWITCH_CLASS_STRUCTURE_ID);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (1900,'ACTCI',1900,0,1,?,'GENERICCOMPUTERSYSTEM_GENERICTYPE')
                """, SWITCH_CLASS_STRUCTURE_ID);
        seedClusterSpec(1901, "COMPUTERSYSTEMCLUSTER_MANAGEDSYSTEMNAME", 1);
        seedClusterSpec(1902, "COMPUTERSYSTEMCLUSTER_LOCATIONTAG", 2);
    }

    private void seedClusterSpec(long id, String attribute, int sequence) {
        jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,'ALN')",
                id, attribute);
        jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES (?,?,?,?)",
                CLUSTER_CLASS_STRUCTURE_ID, id, attribute, id);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (?,'ACTCI',?,0,1,?,?)
                """, id, sequence, CLUSTER_CLASS_STRUCTURE_ID, attribute);
    }

    private DeviceSource source(long id, String type, String name, BigDecimal ram) {
        return new DeviceSource(id, type, "physical".equals(type) ? "Generic" : null, false,
                null, null, null, null, null, name, "description", "serial", "uuid",
                "2026-09-14 00:00:00.123456+00", "model", "manufacturer", ram, "GB",
                2, 12, new BigDecimal("2.40"), "GHz", "Xeon", "x86_64",
                "001122334455", "vm-internal-id", "BIOS vendor", "1.2", "11/12/2021");
    }

    private DeviceSource switchSource(long id, String networkKind, Integer kindCount,
                                      Integer clusterCount) {
        DeviceSource source = source(id, "physical", "Switch", null);
        return new DeviceSource(source.devicePk(), source.type(), "Rackable", true,
                11L, networkKind, kindCount, clusterCount, source.snmpLocation(), source.name(), source.notes(),
                source.serialNo(), source.uuid(), source.lastDiscovered(), source.model(),
                source.manufacturer(), source.ram(), source.ramUnit(), source.totalCpus(),
                source.corePerCpu(), source.cpuSpeed(), source.cpuSpeedUnit(), source.cpuType(),
                source.architecture(), "aabbccddeeff", source.vmId(), source.biosManufacturer(),
                source.biosVersion(), source.biosReleaseDate());
    }

    private DeviceSource clusterSource(long id, String networkKind, String location) {
        DeviceSource source = source(id, "cluster", "Cluster", null);
        return new DeviceSource(source.devicePk(), source.type(), null, true,
                null, networkKind, networkKind == null ? 0 : 1, null, location,
                source.name(), source.notes(), null, null, source.lastDiscovered(), null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private DeviceSource withPhysicalSubtype(DeviceSource source, String physicalSubtype) {
        return new DeviceSource(source.devicePk(), source.type(), physicalSubtype,
                source.networkDevice(), source.clusterPk(), source.networkKind(), source.networkKindCount(),
                source.clusterCount(), source.snmpLocation(), source.name(), source.notes(), source.serialNo(), source.uuid(),
                source.lastDiscovered(), source.model(), source.manufacturer(), source.ram(), source.ramUnit(),
                source.totalCpus(), source.corePerCpu(), source.cpuSpeed(), source.cpuSpeedUnit(),
                source.cpuType(), source.architecture(), source.primaryMac(), source.vmId(),
                source.biosManufacturer(), source.biosVersion(), source.biosReleaseDate());
    }

    @Test
    void mapsPhysicalAndVirtualUsingConfiguredTemplatesAndPreservesUnits() {
        var definitions = definitionLoader.load();
        persist(source(7, "physical", "Physical", new BigDecimal("32.125")), definitions);
        persist(source(8, "virtual", "Virtual", new BigDecimal("16")), definitions);

        assertThat(jdbc.queryForList("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI ORDER BY ACTCINUM", String.class))
                .containsExactly(COMPUTER_CLASS_STRUCTURE_ID, VIRTUAL_CLASS_STRUCTURE_ID);
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
                LocalDateTime.class)).isEqualTo(OffsetDateTime.parse("2026-09-14T00:00:00.123456Z")
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
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
    void mapsOnlyUnambiguousSwitchesToTheGenericSwitchClassification() {
        var definitions = definitionLoader.load();
        persist(switchSource(9, "Switch", 1, 1), definitions);

        assertThat(jdbc.queryForObject(
                "SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:DEVICE:9'",
                String.class)).isEqualTo(SWITCH_CLASS_STRUCTURE_ID);
        assertThat(jdbc.queryForObject("""
                SELECT ALNVALUE FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DEVICE:9'
                  AND ASSETATTRID='GENERICCOMPUTERSYSTEM_GENERICTYPE'
                """, String.class)).isEqualTo("Switch");
        assertThat(text("D42:DEVICE:9", "PRIMARYMACADDRESS", "ALNVALUE"))
                .isEqualTo("aabbccddeeff");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DEVICE:9' AND ASSETATTRID='COMPUTERSYSTEM_VMID'
                """, Integer.class)).isZero();
    }

    @Test
    void mapsSwitchClustersWithOnlyClusterSpecificSpecs() {
        persist(clusterSource(11, "Switch", "DC-1"), definitionLoader.load());

        assertThat(jdbc.queryForObject(
                "SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:DEVICE:11'",
                String.class)).isEqualTo(CLUSTER_CLASS_STRUCTURE_ID);
        assertThat(jdbc.queryForMap("""
                SELECT ALNVALUE FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DEVICE:11'
                  AND ASSETATTRID='COMPUTERSYSTEMCLUSTER_MANAGEDSYSTEMNAME'
                """).get("ALNVALUE")).isEqualTo("Cluster");
        assertThat(jdbc.queryForObject("""
                SELECT ALNVALUE FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DEVICE:11'
                  AND ASSETATTRID='COMPUTERSYSTEMCLUSTER_LOCATIONTAG'
                """, String.class)).isEqualTo("DC-1");
        assertThat(jdbc.queryForList(
                "SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC WHERE ACTCINUM='D42:DEVICE:11'",
                String.class)).containsExactlyInAnyOrder(
                        "COMPUTERSYSTEMCLUSTER_MANAGEDSYSTEMNAME",
                        "COMPUTERSYSTEMCLUSTER_LOCATIONTAG");
    }

    @Test
    void skipsNonSwitchNetworkClusters() {
        assertThat(integration.mapData(
                List.of(clusterSource(11, "Router", null)), definitionLoader.load())).isEmpty();
    }

    @Test
    void skipsRoutersMissingKindsConflictsAndPrinters() {
        var printer = withPhysicalSubtype(switchSource(13, "Printer", 1, 1), "Network Printer");
        var mapped = integration.mapData(List.of(
                switchSource(10, "Router", 1, 1),
                switchSource(11, null, 0, 1),
                switchSource(12, "Switch", 1, 2),
                printer), definitionLoader.load());

        assertThat(mapped).isEmpty();
    }

    @Test
    void rerunKeepsParentAndSpecIdsIncludingNullSections() {
        var definitions = definitionLoader.load();
        persist(source(7, "physical", "Before", new BigDecimal("32")), definitions);
        long parentId = jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class);
        var specIds = jdbc.queryForList("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC ORDER BY ACTCISPECID", Long.class);
        persist(source(7, "physical", "After", new BigDecimal("64")), definitions);

        assertThat(count("ACTCI")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCIID FROM MAXIMO.ACTCI", Long.class)).isEqualTo(parentId);
        assertThat(jdbc.queryForList("SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC ORDER BY ACTCISPECID", Long.class))
                .isEqualTo(specIds);
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("After");
        assertThat(value("D42:DEVICE:7", "MEMORYSIZE", "NUMVALUE")).isEqualByComparingTo("64");
        assertThat(parentId).isNotEqualTo(7);
    }

    @Test
    void mappingDoesNotWriteUntilPutDataIsCalled() {
        var mapped = integration.mapData(
                List.of(source(7, "physical", "Host", BigDecimal.TEN)), definitionLoader.load());
        assertThat(count("ACTCI")).isZero();
        assertThat(count("ACTCISPEC")).isZero();
        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().specs()).isNotEmpty();

        integration.putData(mapped);
        assertThat(count("ACTCI")).isEqualTo(1);
    }

    @Test
    void failedParentDoesNotStopFollowingComputerOrCreateOrphanSpecs() {
        var mapped = integration.mapData(List.of(
                source(7, "physical", "X".repeat(193), BigDecimal.TEN),
                source(8, "virtual", "Good", BigDecimal.ONE)), definitionLoader.load());
        integration.putData(mapped);

        assertThat(jdbc.queryForList("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class))
                .containsExactly("D42:DEVICE:8");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM MAXIMO.ACTCISPEC WHERE ACTCINUM='D42:DEVICE:7'", Integer.class)).isZero();
        assertThat(text("D42:DEVICE:8", "NAME", "ALNVALUE")).isEqualTo("Good");
    }

    @Test
    void failedSpecDoesNotRollBackOrStopFollowingSpecsAndComputers() {
        rejectBiosVersion();
        integration.putData(integration.mapData(List.of(
                source(7, "physical", "A", BigDecimal.TEN),
                source(8, "virtual", "B", BigDecimal.ONE)), definitionLoader.load()));

        assertThat(count("ACTCI")).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID='COMPUTERSYSTEM_ROMVERSION'",
                Integer.class)).isZero();
        for (String ci : List.of("D42:DEVICE:7", "D42:DEVICE:8")) {
            assertThat(text(ci, "NAME", "ALNVALUE")).isNotBlank();
            assertThat(value(ci, "CPUCORESINSTALLED", "NUMVALUE")).isEqualByComparingTo("24");
        }
    }

    @Test
    void failedSpecUpdateKeepsParentUpdateAndContinuesOtherSpecs() {
        var definitions = definitionLoader.load();
        persist(source(7, "physical", "Before", BigDecimal.TEN), definitions);
        jdbc.update("ALTER TABLE MAXIMO.ACTCISPEC ADD CONSTRAINT reject_new_name CHECK (ALNVALUE <> 'After')");
        persist(source(7, "physical", "After", BigDecimal.ONE), definitions);

        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("After");
        assertThat(text("D42:DEVICE:7", "NAME", "ALNVALUE")).isEqualTo("Before");
        assertThat(value("D42:DEVICE:7", "MEMORYSIZE", "NUMVALUE")).isEqualByComparingTo("1");
    }

    @Test
    void missingOrIncompatibleSpecDefinitionOnlySkipsThatAttribute() {
        jdbc.update("DELETE FROM MAXIMO.CLASSSPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        jdbc.update("DELETE FROM MAXIMO.ASSETATTRIBUTE WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        jdbc.update("UPDATE MAXIMO.ASSETATTRIBUTE SET DATATYPE='ALN' WHERE ASSETATTRID='COMPUTERSYSTEM_MEMORYSIZE'");
        jdbc.update("UPDATE MAXIMO.CLASSSPECUSEWITH SET CLASSSTRUCTUREID='WRONG' WHERE ASSETATTRID='COMPUTERSYSTEM_NAME'");
        persist(source(7, "physical", "Host", BigDecimal.TEN), definitionLoader.load());

        assertThat(count("ACTCI")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC", String.class))
                .doesNotContain("COMPUTERSYSTEM_BIOSRELEASEDATE", "COMPUTERSYSTEM_MEMORYSIZE", "COMPUTERSYSTEM_NAME")
                .contains("COMPUTERSYSTEM_ROMVERSION", "COMPUTERSYSTEM_CPUCORESINSTALLED");
    }

    @Test
    void missingComputerClassificationStillAllowsOtherClassifications() {
        jdbc.update("DELETE FROM MAXIMO.CLASSUSEWITH WHERE CLASSSTRUCTUREID=?", COMPUTER_CLASS_STRUCTURE_ID);
        integration.putData(integration.mapData(List.of(
                source(7, "physical", "Physical", BigDecimal.TEN),
                switchSource(8, "Switch", 1, 1)), definitionLoader.load()));
        assertThat(jdbc.queryForList("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class))
                .containsExactly("D42:DEVICE:8");
    }

    @Test
    void readsAllOffsetsEvenWhenAnEntirePageFailsMapping() {
        int batchSize = DeviceCiIntegrate.DEFAULT_BATCH_SIZE;
        List<Long> offsets = new ArrayList<>();
        var task = new DeviceCiIntegrate(mock(Device42ConnectionFactory.class), new ActCiWriter(jdbc),
                new CiSpecMapper()) {
            @Override public long getTotalCount() {
                return batchSize * 2L + 1;
            }
            @Override public List<DeviceSource> getData(long offset, int limit) {
                offsets.add(offset);
                assertThat(limit).isEqualTo(offset == batchSize * 2L ? 1 : batchSize);
                DeviceSource row = source(offset + 1, "physical", "Host", BigDecimal.ONE);
                if (offset == batchSize) {
                    row = withTimeAndUnits(row, "invalid-time", "GB", "GHz");
                }
                return List.of(row);
            }
        };
        task.integrate(definitionLoader.load());
        assertThat(offsets).containsExactly(0L, (long) batchSize, batchSize * 2L);
        assertThat(count("ACTCI")).isEqualTo(2);
    }

    @Test
    void unknownUnitsSkipOnlyMeasuredAttributes() {
        var row = withTimeAndUnits(source(7, "physical", "Host", BigDecimal.ONE),
                "2026-09-14T00:00:00Z", "unknown", "unknown");
        persist(row, definitionLoader.load());

        assertThat(count("ACTCI")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC", String.class))
                .doesNotContain("COMPUTERSYSTEM_MEMORYSIZE", "COMPUTERSYSTEM_CPUSPEED")
                .contains("COMPUTERSYSTEM_NUMCPUS");
    }

    @Test
    void sourceConversionFailureDoesNotDiscardTheRestOfThePage() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(java.sql.Connection.class);
        var statement = mock(java.sql.Statement.class);
        var rs = mock(java.sql.ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getLong("device_pk")).thenReturn(7L, 7L, 8L, 8L);
        when(rs.getBigDecimal("total_cpus")).thenReturn(new BigDecimal("1.5"), BigDecimal.ONE);

        var task = new DeviceCiIntegrate(factory, new ActCiWriter(jdbc), new CiSpecMapper());
        var rows = task.getData(100, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().devicePk()).isEqualTo(8);
        verify(statement).executeQuery(contains("LIMIT 100 OFFSET 100"));
    }

    @Test
    void missingSpecValueCreatesOrUpdatesTemplateRowWithNullValue() {
        var definitions = definitionLoader.load();
        persist(source(7, "physical", "Host", BigDecimal.TEN), definitions);
        persist(source(7, "physical", "Host", null), definitions);
        assertThat(value("D42:DEVICE:7", "MEMORYSIZE", "NUMVALUE")).isNull();
        persist(source(8, "virtual", "Empty", null), definitions);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM MAXIMO.ACTCISPEC WHERE ACTCINUM='D42:DEVICE:8'
                  AND ASSETATTRID='COMPUTERSYSTEM_MEMORYSIZE'
                """, Integer.class)).isEqualTo(1);
        assertThat(value("D42:DEVICE:8", "MEMORYSIZE", "NUMVALUE")).isNull();
    }

    @Test
    void usesCurrentClassificationWhenExistingDeviceTypeChanges() {
        var definitions = definitionLoader.load();
        persist(source(7, "physical", "Physical", BigDecimal.TEN), definitions);
        persist(source(7, "virtual", "Virtual", BigDecimal.ONE), definitions);
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class))
                .isEqualTo(VIRTUAL_CLASS_STRUCTURE_ID);
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("Virtual");
    }

    @Test
    void usesSharedSnapshotWithoutQueryingDefinitionsDuringMapping() {
        var cache = definitionLoader.load();
        jdbc.update("UPDATE MAXIMO.ASSETATTRIBUTE SET DATATYPE='ALN' WHERE ASSETATTRID='COMPUTERSYSTEM_MEMORYSIZE'");
        clearInvocations(jdbc);
        var mapped = integration.mapData(List.of(source(7, "physical", "Host", BigDecimal.TEN)), cache);
        verifyNoInteractions(jdbc);
        assertThat(mapped.getFirst().specs()).anySatisfy(spec -> {
            assertThat(spec.assetAttrId()).isEqualTo("COMPUTERSYSTEM_MEMORYSIZE");
            assertThat(spec.numValue()).isEqualByComparingTo("10");
        });
        var refreshed = definitionLoader.load();
        assertThat(refreshed.spec("PHYS", "COMPUTERSYSTEM_MEMORYSIZE", null).dataType()).isEqualTo("ALN");
        assertThat(cache.spec("PHYS", "COMPUTERSYSTEM_MEMORYSIZE", null).dataType()).isEqualTo("NUMERIC");
    }

    @Test
    void explicitlyAllowedAdditionalAttributeUsesNullTemplateAndLaterAdoptsRegisteredTemplate() {
        jdbc.update("DELETE FROM MAXIMO.CLASSSPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        persist(source(7, "physical", "Host", BigDecimal.TEN), definitionLoader.load());
        var first = jdbc.queryForMap("""
                SELECT ACTCISPECID,CLASSSPECID,DISPLAYSEQUENCE,MANDATORY,SECTION,LINKEDTOATTRIBUTE,ALNVALUE
                FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'
                """);
        assertThat(first.get("CLASSSPECID")).isNull();
        assertThat(((Number) first.get("DISPLAYSEQUENCE")).intValue()).isEqualTo(180);
        assertThat(((Number) first.get("MANDATORY")).intValue()).isZero();
        assertThat(first.get("SECTION")).isNull();
        assertThat(first.get("LINKEDTOATTRIBUTE")).isNull();
        assertThat(first.get("ALNVALUE")).isEqualTo("11/12/2021");

        long attributeId = jdbc.queryForObject(
                "SELECT ASSETATTRIBUTEID FROM MAXIMO.ASSETATTRIBUTE WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'", Long.class);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID)
                VALUES ('PHYS',900,'COMPUTERSYSTEM_BIOSRELEASEDATE',?)
                """, attributeId);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (900,'ACTCI',42,1,1,'PHYS','COMPUTERSYSTEM_BIOSRELEASEDATE')
                """);
        persist(source(7, "physical", "Host", BigDecimal.TEN), definitionLoader.load());
        var second = jdbc.queryForMap("""
                SELECT ACTCISPECID,CLASSSPECID,DISPLAYSEQUENCE,MANDATORY
                FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'
                """);
        assertThat(second.get("ACTCISPECID")).isEqualTo(first.get("ACTCISPECID"));
        assertThat(((Number) second.get("CLASSSPECID")).longValue()).isEqualTo(900);
        assertThat(((Number) second.get("DISPLAYSEQUENCE")).intValue()).isEqualTo(42);
        assertThat(((Number) second.get("MANDATORY")).intValue()).isEqualTo(1);
    }

    @Test
    void doesNotUseAdditionalPathForUnmarkedAttributesOrInvalidExistingTemplates() {
        jdbc.update("DELETE FROM MAXIMO.CLASSSPEC WHERE ASSETATTRID='COMPUTERSYSTEM_NAME'");
        jdbc.update("UPDATE MAXIMO.CLASSSPECUSEWITH SET USEINSPEC=0 WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        var cache = definitionLoader.load();
        assertThat(cache.additionalSpec("PHYS", "COMPUTERSYSTEM_BIOSRELEASEDATE", null, 180, false)).isNull();
        persist(source(7, "physical", "Host", BigDecimal.TEN), cache);
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC", String.class))
                .doesNotContain("COMPUTERSYSTEM_NAME", "COMPUTERSYSTEM_BIOSRELEASEDATE")
                .contains("COMPUTERSYSTEM_ROMVERSION");
    }

    @Test
    void duplicateGlobalAttributeNamesDoNotOverwriteDefinitionsOrEnableAdditionalSpec() {
        jdbc.update("DELETE FROM MAXIMO.CLASSSPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        jdbc.update("""
                INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE)
                VALUES (900,'COMPUTERSYSTEM_BIOSRELEASEDATE','NUMERIC')
                """);
        var cache = definitionLoader.load();
        assertThat(cache.attribute(900).dataType()).isEqualTo("NUMERIC");
        assertThat(cache.additionalSpec("PHYS", "COMPUTERSYSTEM_BIOSRELEASEDATE", null, 180, false)).isNull();
        persist(source(7, "physical", "Host", BigDecimal.TEN), cache);
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC", String.class))
                .doesNotContain("COMPUTERSYSTEM_BIOSRELEASEDATE");
    }

    @Test
    void organizationSpecificAttributeIsNotUsedAsGlobalAdditionalDefinition() {
        jdbc.update("DELETE FROM MAXIMO.CLASSSPEC WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        jdbc.update("UPDATE MAXIMO.ASSETATTRIBUTE SET ORGID='ORG1' WHERE ASSETATTRID='COMPUTERSYSTEM_BIOSRELEASEDATE'");
        assertThat(definitionLoader.load().additionalSpec(
                "PHYS", "COMPUTERSYSTEM_BIOSRELEASEDATE", null, 180, false)).isNull();
    }

    @Test
    void cacheSupportsDifferentPrefixesAndSectionsAndLimitsClassesToRegistry() {
        jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES ('OTHER','SYS.OTHER')");
        jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES ('OTHER','ACTCI')");
        jdbc.update("""
                INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE)
                VALUES (900,'MODELOBJECT_NAME','ALN')
                """);
        for (int i = 0; i < 3; i++) {
            String classId = i == 2 ? "OTHER" : "PHYS";
            String section = i == 0 ? null : "SLOT";
            jdbc.update("""
                    INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID,SECTION)
                    VALUES (?,?,'MODELOBJECT_NAME',900,?)
                    """, classId, 900 + i, section);
            jdbc.update("""
                    INSERT INTO MAXIMO.CLASSSPECUSEWITH
                        (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID,SECTION)
                    VALUES (?,'ACTCI',1,0,1,?,'MODELOBJECT_NAME',?)
                    """, 900 + i, classId, section);
        }
        var cache = definitionLoader.load();
        assertThat(cache.classification(CiClassification.COMPUTER).classStructureId()).isEqualTo("PHYS");
        assertThat(cache.spec("PHYS", "MODELOBJECT_NAME", null).classSpecId()).isEqualTo(900);
        assertThat(cache.spec("PHYS", "MODELOBJECT_NAME", "SLOT").classSpecId()).isEqualTo(901);
        assertThat(cache.spec("OTHER", "MODELOBJECT_NAME", "SLOT")).isNull();
        assertThat(cache.attribute(900)).isNotNull();
    }

    @Test
    void organizationScopedTemplatesAreIgnoredInsteadOfCollidingWithGlobalOnes() {
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID,ORGID)
                VALUES ('PHYS',900,'COMPUTERSYSTEM_NAME',100,'ORG1')
                """);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID,ORGID)
                VALUES (900,'ACTCI',7,1,1,'PHYS','COMPUTERSYSTEM_NAME','ORG1')
                """);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID,ORGID)
                VALUES (101,'ACTCI',9,1,1,'PHYS','COMPUTERSYSTEM_SERIALNUMBER','ORG1')
                """);

        var cache = definitionLoader.load();

        assertThat(cache.spec("PHYS", "COMPUTERSYSTEM_NAME", null).classSpecId()).isEqualTo(100);
        assertThat(cache.spec("PHYS", "COMPUTERSYSTEM_SERIALNUMBER", null).displaySequence()).isEqualTo(101);
        persist(source(7, "physical", "Host", BigDecimal.TEN), cache);
        assertThat(text("D42:DEVICE:7", "NAME", "ALNVALUE")).isEqualTo("Host");
        assertThat(text("D42:DEVICE:7", "SERIALNUMBER", "ALNVALUE")).isEqualTo("serial");
    }

    private void persist(DeviceSource source, CiDefinitionCache definitions) {
        integration.putData(integration.mapData(List.of(source), definitions));
    }

    private DeviceSource withTimeAndUnits(DeviceSource source, String lastDiscovered,
                                          String ramUnit, String speedUnit) {
        return new DeviceSource(source.devicePk(), source.type(), source.physicalSubtype(),
                source.networkDevice(), source.clusterPk(), source.networkKind(), source.networkKindCount(),
                source.clusterCount(), source.snmpLocation(), source.name(), source.notes(), source.serialNo(), source.uuid(),
                lastDiscovered, source.model(), source.manufacturer(), source.ram(), ramUnit,
                source.totalCpus(), source.corePerCpu(), source.cpuSpeed(), speedUnit, source.cpuType(),
                source.architecture(), source.primaryMac(), source.vmId(), source.biosManufacturer(),
                source.biosVersion(), source.biosReleaseDate());
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
