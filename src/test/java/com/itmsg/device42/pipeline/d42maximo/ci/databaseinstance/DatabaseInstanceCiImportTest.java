package com.itmsg.device42.pipeline.d42maximo.ci.databaseinstance;

import com.itmsg.device42.source.device42.ci.databaseinstance.DatabaseInstanceCiQuery;
import com.itmsg.device42.source.device42.ci.databaseinstance.DatabaseInstanceSource;

import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.target.maximo.ci.ActCiWriter;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionLoader;
import com.itmsg.device42.source.device42.Device42ConnectionFactory;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DatabaseInstanceCiImportTest {
    private static final List<String> COMMON_ATTRIBUTES = List.of(
            "APPSERVER_NAME", "APPSERVER_PRODUCTNAME", "APPSERVER_PRODUCTVERSION",
            "APPSERVER_KEYNAME", "DATABASESERVER_HOME");

    private static final Map<CiClassification, String> CLASS_STRUCTURES = new LinkedHashMap<>(Map.of(
            CiClassification.SQL_SERVER, "DBMSSQL",
            CiClassification.DB2_INSTANCE, "DBDB2",
            CiClassification.ORACLE_INSTANCE, "DBORA",
            CiClassification.DATABASE_SERVER, "DBSRV"));

    private JdbcTemplate jdbc;
    private DatabaseInstanceCiMapper mapper;
    private ActCiWriter writer;
    private CiDefinitionLoader definitionLoader;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        definitionLoader = new CiDefinitionLoader(jdbc);
        mapper = new DatabaseInstanceCiMapper(new CiSpecMapper());
        writer = new ActCiWriter(jdbc);
        seedDefinitions();
    }

    private void seedDefinitions() {
        long attributeId = 700;
        for (String attribute : COMMON_ATTRIBUTES) {
            jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,'ALN')",
                    attributeId++, attribute);
        }
        long templateId = 800;
        for (var entry : CLASS_STRUCTURES.entrySet()) {
            String classStructureId = entry.getValue();
            jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES (?,?)",
                    classStructureId, entry.getKey().classificationId());
            jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES (?,'ACTCI')", classStructureId);
            for (String attribute : COMMON_ATTRIBUTES) {
                addTemplate(classStructureId, templateId++, attribute,
                        700 + COMMON_ATTRIBUTES.indexOf(attribute));
            }
        }
    }

    private void addTemplate(String classStructureId, long classSpecId, String attribute, long attributeId) {
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID)
                VALUES (?,?,?,?)
                """, classStructureId, classSpecId, attribute, attributeId);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (?,'ACTCI',?,0,1,?,?)
                """, classSpecId, classSpecId, classStructureId, attribute);
    }

    @Test
    void routesEachEngineToItsOwnClassification() {
        persist(source(872, "DEFAULT", "Microsoft SQL"),
                source(9215, "db2inst1", "DB2"),
                source(9126, ":EPISODE", "Oracle Database"),
                source(9208, "7056622450769928330", "PostgreSQL"));

        assertThat(classStructureOf("D42:DATABASEINSTANCE:872")).isEqualTo("DBMSSQL");
        assertThat(classStructureOf("D42:DATABASEINSTANCE:9215")).isEqualTo("DBDB2");
        assertThat(classStructureOf("D42:DATABASEINSTANCE:9126")).isEqualTo("DBORA");
        assertThat(classStructureOf("D42:DATABASEINSTANCE:9208")).isEqualTo("DBSRV");
    }

    @Test
    void unknownAndMissingEngineFallBackToTheGenericClassification() {
        assertThat(DatabaseInstanceCiMapper.selectClassification("MariaDB"))
                .isEqualTo(CiClassification.DATABASE_SERVER);
        assertThat(DatabaseInstanceCiMapper.selectClassification(null))
                .isEqualTo(CiClassification.DATABASE_SERVER);
        assertThat(DatabaseInstanceCiMapper.selectClassification(" DB2 "))
                .isEqualTo(CiClassification.DB2_INSTANCE);
    }

    @Test
    void mapsBodyAndFiveCommonSpecsFromInstanceAndResource() {
        persist(source(9215, "db2inst1", "DB2"));

        assertThat(jdbc.queryForObject(
                "SELECT ACTCINAME FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:DATABASEINSTANCE:9215'",
                String.class)).isEqualTo("db2inst1");
        assertThat(jdbc.queryForObject(
                "SELECT DESCRIPTION FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:DATABASEINSTANCE:9215'",
                String.class)).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT LASTSCANDT FROM MAXIMO.ACTCI WHERE ACTCINUM='D42:DATABASEINSTANCE:9215'",
                LocalDateTime.class)).isEqualTo(OffsetDateTime.parse("2026-09-11T07:35:22.410045Z")
                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());

        assertThat(jdbc.queryForList("""
                SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DATABASEINSTANCE:9215' ORDER BY ASSETATTRID
                """, String.class))
                .containsExactly("APPSERVER_KEYNAME", "APPSERVER_NAME", "APPSERVER_PRODUCTNAME",
                        "APPSERVER_PRODUCTVERSION", "DATABASESERVER_HOME");
        assertThat(text(9215, "APPSERVER_NAME")).isEqualTo("db2inst1");
        assertThat(text(9215, "APPSERVER_PRODUCTNAME")).isEqualTo("DB2");
        assertThat(text(9215, "APPSERVER_PRODUCTVERSION")).isEqualTo("DB2 v12.1.4.0");
        assertThat(text(9215, "APPSERVER_KEYNAME")).isEqualTo("database_instance|db2inst1|Demo-Server");
        assertThat(text(9215, "DATABASESERVER_HOME")).isEqualTo("/opt/ibm/db2/V11.5");
    }

    @Test
    void keepsTheVersionStringVerbatimWithoutExtractingANumber() {
        String version = "Microsoft SQL Server 2017 (RTM-CU31-GDR) (KB5046061) - 14.0.3480.1 (X64)";
        persist(new DatabaseInstanceSource(872, "DEFAULT", "Microsoft SQL",
                "database_instance|DEFAULT|NBMT", version, "", null, "2026-08-18 07:16:57.265047+00"));

        assertThat(text(872, "APPSERVER_PRODUCTVERSION")).isEqualTo(version);
    }

    /** 범용 분류로 가도 엔진 문자열은 제품명에 남는다. 같은 값을 다른 속성에 복제하지 않는다. */
    @Test
    void theGenericClassificationKeepsTheEngineStringInProductNameOnly() {
        persist(source(9208, "7056622450769928330", "PostgreSQL"));

        assertThat(text(9208, "APPSERVER_PRODUCTNAME")).isEqualTo("PostgreSQL");
        assertThat(jdbc.queryForList("""
                SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DATABASEINSTANCE:9208' ORDER BY ASSETATTRID
                """, String.class))
                .containsExactly("APPSERVER_KEYNAME", "APPSERVER_NAME", "APPSERVER_PRODUCTNAME",
                        "APPSERVER_PRODUCTVERSION", "DATABASESERVER_HOME");
    }

    @Test
    void missingInstallPathStillCreatesTheTemplateRowWithNullValue() {
        persist(new DatabaseInstanceSource(872, "DEFAULT", "Microsoft SQL",
                "database_instance|DEFAULT|NBMT", "14.0.3480.1", "", null,
                "2026-08-18 07:16:57.265047+00"));

        assertThat(jdbc.queryForList("""
                SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM='D42:DATABASEINSTANCE:872' ORDER BY ASSETATTRID
                """, String.class)).contains("DATABASESERVER_HOME");
        assertThat(text(872, "DATABASESERVER_HOME")).isNull();
    }

    @Test
    void missingClassificationSkipsOnlyThatEngine() {
        jdbc.update("DELETE FROM MAXIMO.CLASSUSEWITH WHERE CLASSSTRUCTUREID='DBORA'");

        var mapped = mapper.mapData(
                List.of(source(9126, ":EPISODE", "Oracle Database"), source(9215, "db2inst1", "DB2")),
                definitionLoader.load(CiClassification.ids()));

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().actCi().actCiNum()).isEqualTo("D42:DATABASEINSTANCE:9215");
    }

    @Test
    void unparsableScanTimeSkipsThatInstanceOnly() {
        var bad = new DatabaseInstanceSource(872, "DEFAULT", "Microsoft SQL", "key", "14.0", "", null,
                "not-a-time");

        var mapped = mapper.mapData(List.of(bad, source(9215, "db2inst1", "DB2")),
                definitionLoader.load(CiClassification.ids()));

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().actCi().actCiNum()).isEqualTo("D42:DATABASEINSTANCE:9215");
    }

    @Test
    void readsEveryOffsetUntilTotalCount() {
        int batchSize = DatabaseInstanceCiImport.DEFAULT_BATCH_SIZE;
        List<Long> offsets = new ArrayList<>();
        var query = new DatabaseInstanceCiQuery(new DoqlClient(mock(Device42ConnectionFactory.class))) {
            @Override public long getTotalCount() {
                return batchSize * 2L + 1;
            }
            @Override public List<DatabaseInstanceSource> getData(long offset, int limit) {
                offsets.add(offset);
                assertThat(limit).isEqualTo(offset == batchSize * 2L ? 1 : batchSize);
                return List.of(source(offset + 1, "db2inst1", "DB2"));
            }
        };

        new DatabaseInstanceCiImport(query, mapper, writer).integrate(definitionLoader.load(CiClassification.ids()));

        assertThat(offsets).containsExactly(0L, (long) batchSize, batchSize * 2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCI", Integer.class)).isEqualTo(3);
    }

    private static DatabaseInstanceSource source(long pk, String name, String engine) {
        return new DatabaseInstanceSource(pk, name, engine,
                "database_instance|" + name + "|Demo-Server",
                "DB2".equals(engine) ? "DB2 v12.1.4.0" : engine + " version",
                "", "/opt/ibm/db2/V11.5", "2026-09-11 07:35:22.410045+00");
    }

    private void persist(DatabaseInstanceSource... sources) {
        writer.write(mapper.mapData(List.of(sources), definitionLoader.load(CiClassification.ids())));
    }

    private String classStructureOf(String actCiNum) {
        return jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI WHERE ACTCINUM=?",
                String.class, actCiNum);
    }

    private String text(long pk, String attributeId) {
        return jdbc.queryForObject("""
                SELECT ALNVALUE FROM MAXIMO.ACTCISPEC WHERE ACTCINUM=? AND ASSETATTRID=?
                """, String.class, "D42:DATABASEINSTANCE:" + pk, attributeId);
    }
}
