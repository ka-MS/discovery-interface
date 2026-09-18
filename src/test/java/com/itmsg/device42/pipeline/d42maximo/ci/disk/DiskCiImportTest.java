package com.itmsg.device42.pipeline.d42maximo.ci.disk;

import com.itmsg.device42.source.device42.ci.disk.DiskSource;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.target.maximo.ci.ActCiWriter;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionLoader;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
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

class DiskCiImportTest {
    private JdbcTemplate jdbc;
    private DiskCiMapper mapper;
    private ActCiWriter writer;
    private CiDefinitionLoader definitionLoader;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        definitionLoader = new CiDefinitionLoader(jdbc);
        mapper = new DiskCiMapper(new CiSpecMapper());
        writer = new ActCiWriter(jdbc);
        seedDefinitions();
    }

    private void seedDefinitions() {
        jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES ('DSK',?)",
                CiClassification.DISK_DRIVE.classificationId());
        jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES ('DSK','ACTCI')");
        jdbc.update("INSERT INTO MAXIMO.MEASUREUNIT VALUES ('GBYTE'),('MBYTE')");
        seedSpec(600, "MEDIAACCESSDEVICE_MODEL", "ALN");
        seedSpec(601, "MEDIAACCESSDEVICE_SERIALNUMBER", "ALN");
        seedSpec(602, "DISKDRIVE_DISKSIZE", "NUMERIC");
    }

    private void seedSpec(long id, String attribute, String dataType) {
        jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,?)",
                id, attribute, dataType);
        jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES ('DSK',?,?,?)",
                id, attribute, id);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (?,'ACTCI',?,0,1,'DSK',?)
                """, id, id, attribute);
    }

    @Test
    void mapsBodyAndSpecsWithGigabyteUnit() {
        persist(disk(9, new BigDecimal("978.00"), "GB", "S5RRNF0T366322J"));

        assertThat(jdbc.queryForObject("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class)).isEqualTo("D42:PART:9");
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("Samsung SSD 870");
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class)).isEqualTo("DSK");
        assertThat(number("DISKDRIVE_DISKSIZE")).isEqualByComparingTo("978.00");
        assertThat(unit("DISKDRIVE_DISKSIZE")).isEqualTo("GBYTE");
        assertThat(text("MEDIAACCESSDEVICE_SERIALNUMBER")).isEqualTo("S5RRNF0T366322J");
    }

    @Test
    void convertsTerabyteToGigabyte() {
        persist(disk(10, new BigDecimal("2"), "TB", "SER"));

        assertThat(number("DISKDRIVE_DISKSIZE")).isEqualByComparingTo("2048");
        assertThat(unit("DISKDRIVE_DISKSIZE")).isEqualTo("GBYTE");
    }

    @Test
    void unsupportedUnitSkipsOnlyTheSizeSpec() {
        persist(disk(11, new BigDecimal("500"), "PB", "SER"));

        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC ORDER BY ASSETATTRID", String.class))
                .containsExactly("MEDIAACCESSDEVICE_MODEL", "MEDIAACCESSDEVICE_SERIALNUMBER");
    }

    @Test
    void missingSerialCreatesTemplateRowWithNullValue() {
        persist(disk(12, new BigDecimal("500"), "GB", null));

        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC ORDER BY ASSETATTRID", String.class))
                .containsExactly("DISKDRIVE_DISKSIZE", "MEDIAACCESSDEVICE_MODEL",
                        "MEDIAACCESSDEVICE_SERIALNUMBER");
        assertThat(text("MEDIAACCESSDEVICE_SERIALNUMBER")).isNull();
    }

    @Test
    void missingClassificationSkipsEveryDisk() {
        jdbc.update("DELETE FROM MAXIMO.CLASSUSEWITH WHERE CLASSSTRUCTUREID='DSK'");

        var mapped = mapper.mapData(List.of(disk(9, new BigDecimal("500"), "GB", "SER")), definitionLoader.load(CiClassification.ids()));

        assertThat(mapped).isEmpty();
    }

    private static DiskSource disk(long partPk, BigDecimal size, String unit, String serial) {
        return new DiskSource(partPk, 100, "Samsung SSD 870", serial, "설명", size, unit, "2026-09-15T00:00:00Z");
    }

    private void persist(DiskSource source) {
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
