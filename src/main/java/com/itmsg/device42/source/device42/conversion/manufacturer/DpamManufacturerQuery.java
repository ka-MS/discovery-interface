package com.itmsg.device42.source.device42.conversion.manufacturer;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DpamManufacturerQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;
    private final DeviceSelection parentSelection;
    private final String supplementalName;

    public DpamManufacturerQuery(DeviceSelection selection, DeviceSelection parentSelection, String supplementalName, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
        this.parentSelection = parentSelection;
        this.supplementalName = supplementalName;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(sql(TOTAL_COUNT_QUERY), resultSet -> {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("제조업체 변환 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ManufacturerSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<ManufacturerSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new ManufacturerSource(resultSet.getString("name")));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "제조업체 변환 대상 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql())
                .replace("{{PARENT_FILTER}}", parentSelection.sql())
                .replace("{{SUPPLEMENTAL_NAME}}", DeviceSelection.literal(supplementalName));
    }

    private static final String PARENT_FILTER = "{{PARENT_FILTER}}";

    private static final String COMPUTER_FILTER = "{{COMPUTER_FILTER}}";

    private static final String SOURCE_CTE = """
            WITH target AS (
                SELECT d.device_pk, d.hardware_fk
                FROM view_device_v2 d
                WHERE
            """ + PARENT_FILTER + """
            ),
            computer AS (
                SELECT d.device_pk
                FROM view_device_v2 d
                WHERE
            """ + COMPUTER_FILTER + """
            ),
            names AS (
                SELECT v.name FROM target t
                JOIN view_hardware_v2 h ON h.hardware_pk = t.hardware_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
                UNION
                SELECT v.name FROM view_part_v1 p
                JOIN computer c ON c.device_pk = p.device_fk
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
                UNION
                SELECT v.name FROM view_deviceos_v1 o
                JOIN computer c ON c.device_pk = o.device_fk
                JOIN view_os_v1 s ON s.os_pk = o.os_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
                UNION
                SELECT v.name FROM view_netport_v1 n
                JOIN computer c ON c.device_pk = n.device_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
                UNION
                SELECT v.name FROM view_softwareinuse_v1 u
                JOIN computer c ON c.device_pk = u.device_fk
                JOIN view_software_v1 sw ON sw.software_pk = u.software_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = sw.vendor_fk
                UNION
                SELECT {{SUPPLEMENTAL_NAME}}
            ),
            filtered AS (
                SELECT name FROM names WHERE name IS NOT NULL AND name <> ''
            )
            """;

    private static final String TOTAL_COUNT_QUERY = SOURCE_CTE + """
            SELECT COUNT(*) FROM filtered
            """;

    private static final String SOURCE_QUERY = SOURCE_CTE + """
            SELECT name FROM filtered ORDER BY name
            """;
}
