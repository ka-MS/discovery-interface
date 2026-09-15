package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.DiskSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.DiskSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DiskCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(DiskCiIntegrate.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";
    private static final BigDecimal GB_PER_TB = BigDecimal.valueOf(1024);

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public DiskCiIntegrate(Device42ConnectionFactory connectionFactory, ActCiWriter writer, CiSpecMapper specMapper) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
        this.specMapper = specMapper;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 Disk 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 Disk 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("Disk 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<DiskSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 Disk가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("Disk CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("Disk 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DiskSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<DiskSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long partPk = rs.getLong("part_pk");
                try {
                    data.add(new DiskSource(
                            partPk, rs.getLong("device_fk"), rs.getString("model"),
                            rs.getString("serial_no"), rs.getString("description"),
                            rs.getBigDecimal("hdsize"), rs.getString("hdsize_unit"),
                            rs.getString("last_discovered")));
                } catch (SQLException e) {
                    log.error("Disk 원천 변환에 실패했습니다. partPk={}", partPk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("Disk 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<DiskSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (DiskSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.DISK_DRIVE);
                if (definition == null) {
                    log.warn("Disk 분류가 없어 건너뜁니다. partPk={}", source.partPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:PART:" + source.partPk(), source.model(), definition.classStructureId(),
                        source.description(), SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, DiskSpec.MODEL, source.model(), null);
                specMapper.addSpec(specs, definitions, actCi, DiskSpec.SERIAL_NUMBER, source.serialNo(), null);
                specMapper.addSpec(specs, definitions, actCi, DiskSpec.DISK_SIZE,
                        gigabytes(source.size(), source.sizeUnit()), sizeUnit(source.sizeUnit()));

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("Disk 매핑에 실패했습니다. partPk={}", source.partPk(), e);
            }
        }
        return mappedData;
    }

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
    }

    /** MEASUREUNIT에 TBYTE가 없어 TB는 GB로 환산한다. 미지원 단위는 단위 코드가 없어 스펙이 생략된다. */
    private static BigDecimal gigabytes(BigDecimal size, String unit) {
        if (size == null) {
            return null;
        }
        return "TB".equals(unit == null ? "" : unit.trim()) ? size.multiply(GB_PER_TB) : size;
    }

    private static String sizeUnit(String unit) {
        return switch (unit == null ? "" : unit.trim()) {
            case "GB", "TB" -> "GBYTE";
            default -> null;
        };
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'Hard Disk' AND
            """ + CiSourceFilter.COMPUTER;

    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT p.part_pk, p.device_fk,
                NULLIF(TRIM(pm.name), '') AS model,
                NULLIF(TRIM(p.serial_no), '') AS serial_no,
                NULLIF(TRIM(p.description), '') AS description,
                pm.hdsize, NULLIF(TRIM(pm.hdsize_unit), '') AS hdsize_unit,
                c.last_discovered
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN computer c ON c.device_pk = p.device_fk
            WHERE pm.type_name = 'Hard Disk'
            ORDER BY p.part_pk
            LIMIT %d OFFSET %d
            """;
}
