package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.IpSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IpCiIntegrateTest {
    private JdbcTemplate jdbc;
    private IpCiIntegrate integration;
    private CiDefinitionLoader definitionLoader;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        definitionLoader = new CiDefinitionLoader(jdbc);
        integration = new IpCiIntegrate(mock(Device42ConnectionFactory.class), new ActCiWriter(jdbc), new CiSpecMapper());
        jdbc.update("INSERT INTO MAXIMO.CLASSSTRUCTURE VALUES ('IPA','NET.IPADDRESS')");
        jdbc.update("INSERT INTO MAXIMO.CLASSUSEWITH VALUES ('IPA','ACTCI')");
        seedSpec(800, "IPADDRESS_DOTNOTATION");
        seedSpec(801, "IPADDRESS_STRINGNOTATION");
    }

    private void seedSpec(long id, String attribute) {
        jdbc.update("INSERT INTO MAXIMO.ASSETATTRIBUTE (ASSETATTRIBUTEID,ASSETATTRID,DATATYPE) VALUES (?,?,'ALN')",
                id, attribute);
        jdbc.update("INSERT INTO MAXIMO.CLASSSPEC (CLASSSTRUCTUREID,CLASSSPECID,ASSETATTRID,ASSETATTRIBUTEID) VALUES ('IPA',?,?,?)",
                id, attribute, id);
        jdbc.update("""
                INSERT INTO MAXIMO.CLASSSPECUSEWITH
                    (CLASSSPECID,OBJECTNAME,SEQUENCE,MANDATORY,USEINSPEC,CLASSSTRUCTUREID,ASSETATTRID)
                VALUES (?,'ACTCI',1,0,1,'IPA',?)
                """, id, attribute);
    }

    @Test
    void mapsBodyAndBothAddressSpecsUsingOwnScanTime() {
        integration.putData(integration.mapData(
                List.of(new IpSource(3, 100, "192.168.2.57", "메모", "2026-08-26T07:15:00Z")),
                definitionLoader.load()));

        assertThat(jdbc.queryForObject("SELECT ACTCINUM FROM MAXIMO.ACTCI", String.class)).isEqualTo("D42:IPADDRESS:3");
        assertThat(jdbc.queryForObject("SELECT ACTCINAME FROM MAXIMO.ACTCI", String.class)).isEqualTo("192.168.2.57");
        assertThat(jdbc.queryForObject("SELECT CLASSSTRUCTUREID FROM MAXIMO.ACTCI", String.class)).isEqualTo("IPA");
        assertThat(jdbc.queryForObject("SELECT DESCRIPTION FROM MAXIMO.ACTCI", String.class)).isEqualTo("메모");
        assertThat(jdbc.queryForObject("SELECT LASTSCANDT FROM MAXIMO.ACTCI", LocalDateTime.class))
                .isEqualTo(OffsetDateTime.parse("2026-08-26T07:15:00Z")
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        assertThat(jdbc.queryForList("SELECT ASSETATTRID FROM MAXIMO.ACTCISPEC ORDER BY ASSETATTRID", String.class))
                .containsExactly("IPADDRESS_DOTNOTATION", "IPADDRESS_STRINGNOTATION");
        assertThat(jdbc.queryForList("SELECT DISTINCT ALNVALUE FROM MAXIMO.ACTCISPEC", String.class))
                .containsExactly("192.168.2.57");
    }

    @Test
    void missingClassificationSkipsEveryAddress() {
        jdbc.update("DELETE FROM MAXIMO.CLASSUSEWITH WHERE CLASSSTRUCTUREID='IPA'");

        var mapped = integration.mapData(
                List.of(new IpSource(3, 100, "192.168.2.57", null, "2026-08-26T07:15:00Z")),
                definitionLoader.load());

        assertThat(mapped).isEmpty();
    }

    @Test
    void sourceQueryUsesHostNotationAndDeduplicatesArrayJoin() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(java.sql.Connection.class);
        var statement = mock(java.sql.Statement.class);
        var rs = mock(java.sql.ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        new IpCiIntegrate(factory, new ActCiWriter(jdbc), new CiSpecMapper()).getData(10, 5);

        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(sql.capture());
        assertThat(sql.getValue())
                .contains("HOST(i.ip_address)")
                .contains("DISTINCT ON (i.ipaddress_pk)")
                .contains("LIMIT 5 OFFSET 10");
    }
}
