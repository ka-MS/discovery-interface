package com.itmsg.device42.architecture;

import com.itmsg.device42.pipeline.d42maximo.selection.MaximoSourcePolicy;
import com.itmsg.device42.source.device42.selection.DeviceSelection;
import com.itmsg.device42.source.device42.selection.FilesystemFilter;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.assertThat;

/** H2는 정책의 SQL 3값 논리만 검증한다. DOQL/DB2 실서버 검증이 아니다. */
class SelectionEquivalenceTest {
    @Test
    void policiesKeepEveryCombinationOfNullsTypesSubtypesAndNetworkFlags() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:selection-equivalence")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE TABLE devices(id bigint, type varchar, virtualsubtype_id int, network_device boolean, "
                    + "physicalsubtype varchar, virtualsubtype varchar, fw_device_type varchar)");
            long id = 0;
            for (String type : Arrays.asList(null, "physical", "virtual", "cluster", "other")) {
                for (Integer subtypeId : Arrays.asList(null, 15, 16)) {
                    for (Boolean network : Arrays.asList(null, false, true)) {
                        for (String physical : Arrays.asList(null, "Generic", "Rackable", "Blade", "WorkStation",
                                "ThinClient", "Laptop", "Network Printer", "PDU", "other")) {
                            for (String virtual : Arrays.asList(null, "Internal VM", "Amazon EC2 Instance", "VMWare", "Hyper-V", "other")) {
                                for (String kind : Arrays.asList(null, "Switch", " Switch ", "Router")) {
                                    jdbc.update("INSERT INTO devices VALUES (?,?,?,?,?,?,?)", ++id, type, subtypeId,
                                            network, physical, virtual, kind);
                                }
                            }
                        }
                    }
                }
            }
            String[][] pairs = {
                    {BaselineSql.CpuQuery.DEVICE_FILTER, MaximoSourcePolicy.ASSET_COMPUTER.sql()},
                    {BaselineSql.DeployedAssetQuery.DEVICE_FILTER, MaximoSourcePolicy.ASSET_DEVICE.sql()},
                    {BaselineSql.DpamManufacturerQuery.PARENT_FILTER, MaximoSourcePolicy.ASSET_DEVICE.sql()},
                    {BaselineSql.CiSourceFilter.COMPUTER, MaximoSourcePolicy.CI_COMPUTER.sql()},
                    {BaselineSql.CiSourceFilter.DEVICE, MaximoSourcePolicy.CI_DEVICE.sql()},
                    {"d.network_device = true AND d.type = 'physical'", MaximoSourcePolicy.ASSET_NETWORK.sql()},
                    {"d.type IN ('virtual', 'physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15) "
                            + "AND (d.network_device = false OR d.network_device IS NULL) AND d.physicalsubtype = 'Network Printer'",
                            MaximoSourcePolicy.ASSET_PRINTER.sql()}
            };
            for (String[] pair : pairs) {
                assertThat(ids(jdbc, pair[1])).as(pair[1]).isEqualTo(ids(jdbc, pair[0]));
            }
            assertThat(ids(jdbc, DeviceSelection.all().sql())).hasSize((int) id);
        }
    }

    @Test
    void filesystemNullCaseAndDifferentAssetCiExclusionsArePreserved() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:filesystem-equivalence")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE TABLE mounts(fstype_name varchar)");
            for (String name : Arrays.asList(null, "", "overlay", "OVERLAY", "squashfs", "devtmpfs", "efivarfs", "xfs")) {
                jdbc.update("INSERT INTO mounts VALUES (?)", name);
            }
            assertThat(names(jdbc, MaximoSourcePolicy.CI_FILESYSTEM.sql(false)))
                    .isEqualTo(names(jdbc, "(m.fstype_name IS NULL OR m.fstype_name NOT IN ('overlay','squashfs','efivarfs'))"));
            assertThat(names(jdbc, MaximoSourcePolicy.ASSET_FILESYSTEM.sql(true)))
                    .isEqualTo(names(jdbc, "LOWER(COALESCE(m.fstype_name,'')) NOT IN ('overlay','devtmpfs','efivarfs')"));
            assertThat(names(jdbc, new FilesystemFilter(List.of()).sql(false))).hasSize(8);
            assertThat(DeviceSelection.literal("a'b")).isEqualTo("'a''b'");
        }
    }

    private static List<Long> ids(JdbcTemplate jdbc, String predicate) {
        // JSON extraction itself is unchanged; H2 fixture stores that source field as a scalar.
        return jdbc.queryForList("SELECT id FROM devices d WHERE "
                + predicate.replace("d.details->>'fw_device_type'", "d.fw_device_type") + " ORDER BY id", Long.class);
    }

    private static List<String> names(JdbcTemplate jdbc, String predicate) {
        return jdbc.queryForList("SELECT fstype_name FROM mounts m WHERE " + predicate + " ORDER BY fstype_name", String.class);
    }
}
