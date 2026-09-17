package com.itmsg.device42.integration.d42maximo.ci.filesystem;

import com.itmsg.device42.device42.DoqlClient;
import com.itmsg.device42.integration.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.maximo.ci.ActCiWriter;
import com.itmsg.device42.maximo.ci.definition.CiDefinitionLoader;
import com.itmsg.device42.device42.Device42ConnectionFactory;
import com.itmsg.device42.integration.d42maximo.ci.mapping.CiClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FilesystemCiImportTest {
    private JdbcTemplate jdbc;
    private FilesystemCiMapper mapper;
    private ActCiWriter writer;
    private CiDefinitionLoader definitionLoader;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        definitionLoader = new CiDefinitionLoader(jdbc);
        mapper = new FilesystemCiMapper(new CiSpecMapper());
        writer = new ActCiWriter(jdbc);
        seedDefinitions();
    }

    private void seedDefinitions() {
        jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES ('FS',?)",
                CiClassification.FILE_SYSTEM.classificationId());
        jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES ('FS','ACTCI')");
        jdbc.update("INSERT INTO MAXIMO.MEASUREUNIT VALUES ('MBYTE')");
        seedSpec(700, "FILESYSTEM_MOUNTPOINT", "ALN");
        seedSpec(701, "FILESYSTEM_TYPE", "ALN");
        seedSpec(702, "FILESYSTEM_CAPACITY", "NUMERIC");
        seedSpec(703, "FILESYSTEM_AVAILABLESPACE", "NUMERIC");
        seedSpec(704, "MODELOBJECT_LABEL", "ALN");
    }

    private void seedSpec(long id, String attribute, String dataType) {
        jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,?)",
                id, attribute, dataType);
        jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES ('FS',?,?,?)",
                id, attribute, id);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (?,'ACTCI',?,0,1,'FS',?)
                """, id, id, attribute);
    }

    @Test
    void mapsBodyAndSpecsWithMegabyteUnit() {
        persist(new FilesystemSource(4, 100, "/", "xfs", "root-vol",
                new BigDecimal("122673.00"), new BigDecimal("93352.00"), "2026-09-15T00:00:00Z"));

        assertThat(jdbc.queryForObject("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class)).isEqualTo("D42:MOUNTPOINT:4");
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("/");
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class)).isEqualTo("FS");
        assertThat(number("FILESYSTEM_CAPACITY")).isEqualByComparingTo("122673.00");
        assertThat(unit("FILESYSTEM_CAPACITY")).isEqualTo("MBYTE");
        assertThat(unit("FILESYSTEM_AVAILABLESPACE")).isEqualTo("MBYTE");
        assertThat(text("FILESYSTEM_TYPE")).isEqualTo("xfs");
        assertThat(text("MODELOBJECT_LABEL")).isEqualTo("root-vol");
    }

    @Test
    void missingValuesCreateTemplateRowsWithNullValues() {
        persist(new FilesystemSource(5, 100, "/boot", "ext4", null, null, null, "2026-09-15T00:00:00Z"));

        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC ORDER BY ASSETATTRID", String.class))
                .containsExactly("FILESYSTEM_AVAILABLESPACE", "FILESYSTEM_CAPACITY",
                        "FILESYSTEM_MOUNTPOINT", "FILESYSTEM_TYPE", "MODELOBJECT_LABEL");
        assertThat(number("FILESYSTEM_CAPACITY")).isNull();
        assertThat(number("FILESYSTEM_AVAILABLESPACE")).isNull();
        assertThat(text("MODELOBJECT_LABEL")).isNull();
    }

    @Test
    void sourceQueryExcludesContainerAndVirtualFilesystems() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(java.sql.Connection.class);
        var statement = mock(java.sql.Statement.class);
        var rs = mock(java.sql.ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        new FilesystemCiQuery(new DoqlClient(factory)).getData(0, 10);

        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(sql.capture());
        assertThat(sql.getValue())
                .contains("m.label")
                .contains("'overlay'").contains("'squashfs'").contains("'efivarfs'")
                .doesNotContain("'devtmpfs'")
                .contains("NOT IN");
    }

    @Test
    void missingClassificationSkipsEveryFilesystem() {
        jdbc.update("DELETE FROM MAXIMO.CLASSUSEWITH WHERE CLASSSTRUCTUREID='FS'");

        var mapped = mapper.mapData(List.of(new FilesystemSource(4, 100, "/", "xfs", null,
                BigDecimal.TEN, BigDecimal.ONE, "2026-09-15T00:00:00Z")), definitionLoader.load(CiClassification.ids()));

        assertThat(mapped).isEmpty();
    }

    private void persist(FilesystemSource source) {
        writer.write(mapper.mapData(List.of(source), definitionLoader.load(CiClassification.ids())));
    }

    private String text(String attributeId) {
        return jdbc.queryForObject("SELECT ALNVALUE FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID=?", String.class, attributeId);
    }

    private BigDecimal number(String attributeId) {
        return jdbc.queryForObject("SELECT NUMVALUE FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID=?", BigDecimal.class, attributeId);
    }

    private String unit(String attributeId) {
        return jdbc.queryForObject("SELECT MEASUREUNITID FROM MAXIMO.ACTCISPEC WHERE ASSETATTRID=?", String.class, attributeId);
    }
}
