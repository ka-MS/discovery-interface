package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.OsSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.OsSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OsCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(OsCiIntegrate.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public OsCiIntegrate(Device42ConnectionFactory connectionFactory, ActCiWriter writer, CiSpecMapper specMapper) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
        this.specMapper = specMapper;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 OS 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 OS 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("OS 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<OsSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 OS가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("OS CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("OS 건수 조회에 실패했습니다.", e);
        }
    }

    public List<OsSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<OsSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long deviceOsPk = rs.getLong("deviceos_pk");
                try {
                    data.add(new OsSource(
                            deviceOsPk, rs.getLong("device_fk"), rs.getString("os_name"),
                            rs.getString("os_version"), rs.getString("os_version_no"),
                            rs.getString("os_arch_name"), rs.getString("last_discovered")));
                } catch (SQLException e) {
                    log.error("OS 원천 변환에 실패했습니다. deviceOsPk={}", deviceOsPk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("OS 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<OsSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (OsSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.OPERATING_SYSTEM);
                if (definition == null) {
                    log.warn("OS 분류가 없어 건너뜁니다. deviceOsPk={}", source.deviceOsPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:DEVICEOS:" + source.deviceOsPk(), source.osName(), definition.classStructureId(),
                        null, SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, OsSpec.OS_NAME, source.osName(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.NAME, source.osName(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.OS_VERSION, source.osVersion(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.KERNEL_VERSION, source.kernelVersion(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.KERNEL_ARCHITECTURE, source.architecture(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("OS 매핑에 실패했습니다. deviceOsPk={}", source.deviceOsPk(), e);
            }
        }
        return mappedData;
    }

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            WHERE
            """ + CiSourceFilter.COMPUTER;

    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT o.deviceos_pk, o.device_fk,
                NULLIF(TRIM(o.os_name), '') AS os_name,
                NULLIF(TRIM(o.os_version), '') AS os_version,
                NULLIF(TRIM(o.os_version_no), '') AS os_version_no,
                NULLIF(TRIM(o.os_arch_name), '') AS os_arch_name,
                c.last_discovered
            FROM view_deviceos_v1 o
            JOIN computer c ON c.device_pk = o.device_fk
            ORDER BY o.deviceos_pk
            LIMIT %d OFFSET %d
            """;
}
