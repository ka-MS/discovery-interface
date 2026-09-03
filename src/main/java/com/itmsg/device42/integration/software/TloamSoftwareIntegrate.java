package com.itmsg.device42.integration.software;

import com.itmsg.device42.dto.device42.software.SoftwareProductSource;
import com.itmsg.device42.dto.maximo.software.TloamSoftwareUpsert;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Order(1)
public class TloamSoftwareIntegrate implements SoftwareIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(TloamSoftwareIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String UNKNOWN = "UNKNOWN";

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public TloamSoftwareIntegrate(
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
            log.info("배치할 TLOAM 소프트웨어 카탈로그 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        log.info("배치할 TLOAM 소프트웨어 카탈로그 총 데이터. totalcount={}", totalCount);

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<SoftwareProductSource> data = getData(offset, limit);

            List<TloamSoftwareUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("TLOAM 소프트웨어 카탈로그 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<SoftwareProductSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<SoftwareProductSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new SoftwareProductSource(
                        resultSet.getString("software_name"),
                        resultSet.getString("version"),
                        resultSet.getString("manufacturer")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "TLOAM 소프트웨어 카탈로그 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<TloamSoftwareUpsert> mapData(List<SoftwareProductSource> data) {
        List<TloamSoftwareUpsert> mappedData = new ArrayList<>(data.size());

        for (SoftwareProductSource source : data) {
            String softwareName = defaultUnknown(source.softwareName());
            String manufacturer = defaultUnknown(source.manufacturer());
            String version = trimToNull(source.version());

            mappedData.add(new TloamSoftwareUpsert(
                    buildUniqueId(softwareName, version, manufacturer),
                    softwareName,
                    manufacturer,
                    version
            ));
        }

        return mappedData;
    }

    public void putData(List<TloamSoftwareUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_TLOAM_SOFTWARE_QUERY,
                (PreparedStatement statement) -> {
                    for (TloamSoftwareUpsert software : data) {
                        try {
                            statement.setString(1, software.uniqueId());
                            statement.setString(2, software.softwareName());
                            statement.setString(3, software.manufacturer());
                            statement.setString(4, software.version());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "TLOAM 소프트웨어 카탈로그 MERGE에 실패했습니다. uniqueId={}",
                                    software.uniqueId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static String buildUniqueId(String softwareName, String version, String manufacturer) {
        String nameToken = defaultUnknown(softwareName).toUpperCase(Locale.ROOT);
        String versionToken = defaultUnknown(version).replace(" ", "").toUpperCase(Locale.ROOT);
        String manufacturerToken = defaultUnknown(manufacturer).toUpperCase(Locale.ROOT);
        return nameToken + "|" + versionToken + "|" + manufacturerToken;
    }

    private static String defaultUnknown(String value) {
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

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

    private static final String CATALOG_SOURCE_QUERY = """
            SELECT DISTINCT
                COALESCE(NULLIF(TRIM(s.name), ''), 'UNKNOWN') AS software_name,
                NULLIF(TRIM(u.version), '') AS version,
                COALESCE(NULLIF(TRIM(v.name), ''), 'UNKNOWN') AS manufacturer
            FROM view_softwareinuse_v1 u
            JOIN view_device_v2 d ON d.device_pk = u.device_fk
            LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM (
            """ + CATALOG_SOURCE_QUERY + """
            ) catalog
            """;

    private static final String SOURCE_QUERY = CATALOG_SOURCE_QUERY + """
            ORDER BY software_name, version, manufacturer
            """;

    private static final String MERGE_TLOAM_SOFTWARE_QUERY = """
            MERGE INTO MAXIMO.TLOAMSOFTWARE AS target
            USING (
                VALUES (?, ?, ?, ?)
            ) AS source (
                UNIQUEID,
                SWNAME,
                MANUFACTURER,
                VERSION
            )
            ON target.UNIQUEID = source.UNIQUEID
            WHEN NOT MATCHED THEN
                INSERT (
                    TLOAMSOFTWAREID,
                    UNIQUEID,
                    SWNAME,
                    MANUFACTURER,
                    VERSION,
                    RELEASE,
                    ROLE,
                    ISIPLA,
                    ISPVU,
                    ISSUBCAP,
                    ISDELETED,
                    ISREVIEWED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.TLOAMSOFTWARESEQ,
                    source.UNIQUEID,
                    source.SWNAME,
                    source.MANUFACTURER,
                    source.VERSION,
                    NULL,
                    'SOFTWAREPRODUCT',
                    0,
                    0,
                    0,
                    0,
                    0
                )
            """;
}
