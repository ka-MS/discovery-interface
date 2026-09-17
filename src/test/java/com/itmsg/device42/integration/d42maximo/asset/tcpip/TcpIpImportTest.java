package com.itmsg.device42.integration.d42maximo.asset.tcpip;

import com.itmsg.device42.device42.DoqlClient;
import com.itmsg.device42.maximo.asset.DpaTcpIpWriter;

import com.itmsg.device42.device42.Device42ConnectionFactory;
import com.itmsg.device42.maximo.asset.DpaTcpIpUpsert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TcpIpImportTest {

    @Test
    void convertsPrefixLengthToIpv4Netmask() {
        assertThat(TcpIpMapper.toIpv4Netmask(20)).isEqualTo("255.255.240.0");
        assertThat(TcpIpMapper.toIpv4Netmask(22)).isEqualTo("255.255.252.0");
        assertThat(TcpIpMapper.toIpv4Netmask(24)).isEqualTo("255.255.255.0");
        assertThat(TcpIpMapper.toIpv4Netmask(32)).isEqualTo("255.255.255.255");
    }

    @Test
    void treatsCatchAllAndMissingPrefixAsNoNetmask() {
        assertThat(TcpIpMapper.toIpv4Netmask(0)).isNull();
        assertThat(TcpIpMapper.toIpv4Netmask(null)).isNull();
    }

    @Test
    void rejectsInvalidIpv4PrefixLength() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TcpIpMapper.toIpv4Netmask(33));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TcpIpMapper.toIpv4Netmask(-1));
    }

    @Test
    void sourceQueryExpandsEveryDeviceAndSelectsReusableIpFields() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        var resultSet = mock(ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        new TcpIpQuery(new DoqlClient(factory)).getData(10, 5);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(sql.capture());
        assertThat(sql.getValue())
                .contains("d.device_pk = ANY(i.device_fks)")
                .contains("i.subnet_fk")
                .contains("i.netport_fk")
                .contains("i.is_shared")
                .contains("i.last_discovered")
                .doesNotContain("DISTINCT ON (i.ipaddress_pk)")
                .contains("ORDER BY i.ipaddress_pk, d.device_pk")
                .contains("LIMIT 5 OFFSET 10");
    }

    @Test
    void totalCountCountsDeviceIpPairsRatherThanDistinctAddresses() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        var resultSet = mock(ResultSet.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(118L);

        assertThat(new TcpIpQuery(new DoqlClient(factory)).getTotalCount()).isEqualTo(118L);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue())
                .contains("SELECT COUNT(*)")
                .contains("d.device_pk = ANY(i.device_fks)")
                .doesNotContain("COUNT(DISTINCT");
    }

    @Test
    void mergeStoresSharedAddressForEveryNodeAndKeepsGeneratedIdsOnRerun() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA MAXIMO");
        jdbc.execute("CREATE SEQUENCE MAXIMO.DPATCPIPSEQ START WITH 1 INCREMENT BY 1");
        jdbc.execute("""
                CREATE TABLE MAXIMO.DPATCPIP (
                    TCPIPID BIGINT PRIMARY KEY,
                    GATEWAY VARCHAR(32),
                    HOST VARCHAR(128),
                    NODEID BIGINT NOT NULL,
                    TCPIPADDRESS VARCHAR(39) NOT NULL,
                    TCPIPNETMASK VARCHAR(32),
                    CREATEDATE TIMESTAMP NOT NULL,
                    CHANGEDATE TIMESTAMP NOT NULL
                )
                """);
        var writer = new DpaTcpIpWriter(jdbc);
        LocalDateTime firstRun = LocalDateTime.of(2026, 9, 17, 10, 0);

        writer.write(List.of(
                tcpIp(180, "host-180", "192.168.122.1", firstRun),
                tcpIp(257, "host-257", "192.168.122.1", firstRun),
                tcpIp(189, "host-189", "192.168.122.1", firstRun)
        ));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.DPATCPIP", Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForList(
                "SELECT NODEID FROM MAXIMO.DPATCPIP ORDER BY NODEID", Long.class))
                .containsExactly(180L, 189L, 257L);
        Long originalId = jdbc.queryForObject(
                "SELECT TCPIPID FROM MAXIMO.DPATCPIP WHERE NODEID=180", Long.class);

        LocalDateTime secondRun = firstRun.plusHours(1);
        writer.write(List.of(tcpIp(180, "renamed-180", "192.168.122.1", secondRun)));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.DPATCPIP", Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject(
                "SELECT TCPIPID FROM MAXIMO.DPATCPIP WHERE NODEID=180", Long.class)).isEqualTo(originalId);
        assertThat(jdbc.queryForObject(
                "SELECT HOST FROM MAXIMO.DPATCPIP WHERE NODEID=180", String.class)).isEqualTo("renamed-180");
    }

    private static DpaTcpIpUpsert tcpIp(long nodeId, String host, String address, LocalDateTime timestamp) {
        return new DpaTcpIpUpsert(
                nodeId, null, host, address, "255.255.255.0", timestamp, timestamp);
    }
}
