package com.itmsg.device42.architecture;

import com.itmsg.device42.pipeline.d42maximo.asset.netprinter.NetPrinterMapper;
import com.itmsg.device42.pipeline.d42maximo.selection.MaximoSourcePolicy;
import com.itmsg.device42.pipeline.d42maximo.software.catalog.TloamSoftwareMapper;
import com.itmsg.device42.pipeline.d42maximo.software.mapping.SoftwareIdentity;
import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.source.device42.asset.netprinter.NetworkPrinterSource;
import com.itmsg.device42.source.device42.software.catalog.SoftwareCatalogQuery;
import com.itmsg.device42.target.maximo.software.TloamSoftwareUpsert;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProjectionEquivalenceTest {
    @Test
    void catalogKeepsDefaultEquivalentGroupsCountOrderAndPagesButMapperProducesTargetDefaults() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:catalog-projection")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE TABLE view_device_v2(device_pk bigint,type varchar,virtualsubtype_id int,network_device boolean,physicalsubtype varchar)");
            jdbc.execute("CREATE TABLE view_vendor_v1(vendor_pk bigint,name varchar)");
            jdbc.execute("CREATE TABLE view_software_v1(software_pk bigint,name varchar,vendor_fk bigint)");
            jdbc.execute("CREATE TABLE view_softwareinuse_v1(software_fk bigint,device_fk bigint,version varchar)");
            jdbc.execute("INSERT INTO view_device_v2 VALUES (1,'physical',NULL,false,'Generic'),(2,'virtual',15,false,NULL),(3,'physical',NULL,false,'PDU')");
            jdbc.execute("INSERT INTO view_vendor_v1 VALUES (1,NULL),(2,' '),(3,'UNKNOWN'),(4,' Zeta '),(5,'Alpha')");
            jdbc.execute("INSERT INTO view_software_v1 VALUES (1,NULL,1),(2,' ',2),(3,'UNKNOWN',3),(4,' Agent ',4),(5,'Zebra',5),(6,'Agent',NULL)");
            jdbc.execute("INSERT INTO view_softwareinuse_v1 VALUES (1,1,'1'),(2,1,'1'),(3,1,'1'),(4,1,'1'),(4,1,' 1 '),(4,1,'2'),(5,1,NULL),(6,1,''),(6,1,NULL),(NULL,1,NULL),(5,2,'excluded'),(5,3,'excluded')");
            var doql = mock(DoqlClient.class);
            var read = (org.mockito.stubbing.Answer<Object>) invocation -> {
                try (var statement = connection.createStatement();
                     var resultSet = statement.executeQuery(invocation.getArgument(0, String.class))) {
                    return invocation.getArgument(1, DoqlClient.ResultReader.class).read(resultSet);
                }
            };
            when(doql.query(anyString(), any())).thenAnswer(read);
            when(doql.preparedQuery(anyString(), any())).thenAnswer(read);
            var query = new SoftwareCatalogQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.UNKNOWN, doql);
            long count = query.getTotalCount();
            assertThat(count).isEqualTo(jdbc.queryForObject(BaselineSql.TloamSoftwareQuery.TOTAL_COUNT_QUERY, Long.class));
            assertThat(count).isGreaterThan(2);
            var mapper = new TloamSoftwareMapper();
            boolean rawMissingSeen = false;
            for (long offset = 0; offset < count + 2; offset += 2) {
                var expected = jdbc.query(BaselineSql.TloamSoftwareQuery.SOURCE_QUERY + " LIMIT 2 OFFSET " + offset,
                        (rs, n) -> new TloamSoftwareUpsert(
                                SoftwareIdentity.buildUniqueId(rs.getString(1), rs.getString(2), rs.getString(3)),
                                rs.getString(1), rs.getString(3), rs.getString(2)));
                var source = query.getData(offset, 2);
                rawMissingSeen |= source.stream().anyMatch(row -> row.softwareName() == null || row.manufacturer() == null);
                assertThat(mapper.mapData(source)).as("offset %s", offset).isEqualTo(expected);
            }
            assertThat(rawMissingSeen).isTrue();
        }
    }

    @Test
    void printerMacUppercaseMovesFromSqlToMapperWithoutChangingValues() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:printer-projection")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            var mapper = new NetPrinterMapper();
            for (String mac : Arrays.asList(null, "", "aa:bb:00:12:ff:ee", "AA-BB-FF", " aabbcc ")) {
                String expected = jdbc.queryForObject("SELECT UPPER(CAST(? AS varchar))", String.class, mac);
                var raw = new NetworkPrinterSource(1, null, null, mac, null, 1);
                assertThat(mapper.mapData(List.of(raw)).getFirst().netMacAddress()).isEqualTo(expected);
            }
        }
    }
}
