package com.itmsg.device42.integration.d42maximo.conversion.manuvariant;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.conversion.ManufacturerSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DpamManuVariantQuery {

    public DpamManuVariantQuery(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private final Device42ConnectionFactory connectionFactory;

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("제조업체 변환 변형 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ManufacturerSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<ManufacturerSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new ManufacturerSource(resultSet.getString("name")));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "제조업체 변환 변형 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static final String PARENT_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
            """;

    private static final String COMPUTER_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

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
                SELECT 'UNKNOWN'
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
