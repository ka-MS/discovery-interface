package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.asset.MediaAdapterSource;
import com.itmsg.device42.dto.maximo.DpaMediaAdapterUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(8)
public class DpaMediaAdapterIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaMediaAdapterIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String UNKNOWN = "UNKNOWN";
    private static final String MEDIA_TYPE = "Video";

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaMediaAdapterIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    @Override
    public void integrate() {
        long totalCount = getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA 미디어 어댑터 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<MediaAdapterSource> data = getData(offset, limit);

            List<DpaMediaAdapterUpsert> mappedData = mapData(data);

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA 미디어 어댑터 대상 파트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<MediaAdapterSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<MediaAdapterSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new MediaAdapterSource(
                        resultSet.getLong("part_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("serial_no"),
                        resultSet.getString("description"),
                        resultSet.getString("model_name"),
                        resultSet.getString("model_description"),
                        resultSet.getString("vendor_name")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA 미디어 어댑터 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaMediaAdapterUpsert> mapData(List<MediaAdapterSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaMediaAdapterUpsert> mappedData = new ArrayList<>(data.size());

        for (MediaAdapterSource source : data) {
            mappedData.add(new DpaMediaAdapterUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    firstNonBlank(
                            source.description(),
                            source.modelDescription(),
                            source.modelName()
                    ),
                    defaultUnknown(source.modelName()),
                    defaultUnknown(source.vendorName()),
                    MEDIA_TYPE,
                    trimToNull(source.serialNo()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaMediaAdapterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_MEDIA_ADAPTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaMediaAdapterUpsert adapter : data) {
                        try {
                            statement.setLong(1, adapter.adapterId());
                            statement.setString(2, adapter.description());
                            statement.setString(3, adapter.makeModel());
                            statement.setString(4, adapter.manufacturer());
                            statement.setString(5, adapter.mediaType());
                            statement.setLong(6, adapter.nodeId());
                            statement.setString(7, adapter.serialNumber());
                            statement.setTimestamp(8, toTimestamp(adapter.createDate()));
                            statement.setTimestamp(9, toTimestamp(adapter.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA 미디어 어댑터 MERGE에 실패했습니다. adapterId={}, nodeId={}",
                                    adapter.adapterId(),
                                    adapter.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    static String defaultUnknown(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? UNKNOWN : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
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
            WHERE pm.type_name = 'GPU'
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
                pm.description AS model_description,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, p.part_pk
            """;

    private static final String MERGE_DPA_MEDIA_ADAPTER_QUERY = """
            MERGE INTO MAXIMO.DPAMEDIAADAPTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                ADAPTERID,
                DESCRIPTION,
                MAKEMODEL,
                MANUFACTURER,
                MEDIATYPE,
                NODEID,
                SERIALNUMBER,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.ADAPTERID = source.ADAPTERID
            WHEN MATCHED THEN
                UPDATE SET
                    DESCRIPTION = source.DESCRIPTION,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    MEDIATYPE = source.MEDIATYPE,
                    NODEID = source.NODEID,
                    SERIALNUMBER = source.SERIALNUMBER,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ADAPTERID,
                    DESCRIPTION,
                    MAKEMODEL,
                    MANUFACTURER,
                    MEDIATYPE,
                    NODEID,
                    SERIALNUMBER,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.ADAPTERID,
                    source.DESCRIPTION,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.MEDIATYPE,
                    source.NODEID,
                    source.SERIALNUMBER,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
