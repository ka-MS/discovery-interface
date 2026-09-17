package com.itmsg.device42.integration.d42maximo.asset.disk;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.asset.DiskSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiskQuery {

    public DiskQuery(Device42ConnectionFactory connectionFactory) {
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
            throw new IllegalStateException("DPA Disk 대상 파트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DiskSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<DiskSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new DiskSource(
                        resultSet.getLong("part_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("serial_no"),
                        resultSet.getString("description"),
                        resultSet.getString("model_name"),
                        resultSet.getBigDecimal("hdsize"),
                        resultSet.getString("hdsize_unit"),
                        resultSet.getString("hddtype_name"),
                        resultSet.getString("media_type_name"),
                        resultSet.getString("vendor_name")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA Disk 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

    private static final String SOURCE_FROM_AND_FILTER = """
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
            WHERE pm.type_name = 'Hard Disk'
              AND
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT
                p.part_pk,
                p.device_fk,
                p.serial_no,
                p.description,
                pm.name AS model_name,
                pm.hdsize,
                pm.hdsize_unit,
                pm.hddtype_name,
                pm.media_type_name,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, pm.name, p.part_pk
            """;
}
