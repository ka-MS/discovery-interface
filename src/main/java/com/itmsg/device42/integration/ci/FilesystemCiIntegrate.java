package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.FilesystemSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.FilesystemSpec;
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
import java.util.stream.Collectors;

@Component
public class FilesystemCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(FilesystemCiIntegrate.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";

    /** 용량 단위 컬럼이 원천에 없다. 표본상 MB이며 근거는 원천 조사 문서에 있다. */
    private static final String CAPACITY_UNIT = "MBYTE";

    /**
     * 적재하지 않는 파일시스템 종류. 컨테이너 런타임·이미지 마운트다.
     * 경로에 컨테이너 ID가 들어가 재기동 시 원천 PK가 바뀌면 매 실행마다 새 CI가 쌓인다.
     * devtmpfs는 경로가 고정돼 이 문제가 없어 수집 대상이다.
     */
    public static final List<String> EXCLUDED_TYPES = List.of("overlay", "squashfs", "efivarfs");

    /** EXCLUDED_TYPES를 DOQL IN 절에 넣을 수 있게 join한 문자열. CiRelationSource도 재사용한다. */
    public static final String EXCLUDED_TYPES_SQL = EXCLUDED_TYPES.stream()
            .map(type -> "'" + type + "'").collect(Collectors.joining(", "));

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public FilesystemCiIntegrate(Device42ConnectionFactory connectionFactory, ActCiWriter writer, CiSpecMapper specMapper) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
        this.specMapper = specMapper;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 Filesystem 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 Filesystem 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("Filesystem 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<FilesystemSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 Filesystem이 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("Filesystem CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("Filesystem 건수 조회에 실패했습니다.", e);
        }
    }

    public List<FilesystemSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<FilesystemSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long mountPointPk = rs.getLong("mountpoint_pk");
                try {
                    data.add(new FilesystemSource(
                            mountPointPk, rs.getLong("device_fk"), rs.getString("mountpoint"),
                            rs.getString("fstype_name"), rs.getString("label"), rs.getBigDecimal("capacity"),
                            rs.getBigDecimal("free_capacity"), rs.getString("last_discovered")));
                } catch (SQLException e) {
                    log.error("Filesystem 원천 변환에 실패했습니다. mountPointPk={}", mountPointPk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("Filesystem 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<FilesystemSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (FilesystemSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.FILE_SYSTEM);
                if (definition == null) {
                    log.warn("Filesystem 분류가 없어 건너뜁니다. mountPointPk={}", source.mountPointPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:MOUNTPOINT:" + source.mountPointPk(), source.mountPoint(), definition.classStructureId(),
                        null, SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.MOUNT_POINT, source.mountPoint(), null);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.TYPE, source.type(), null);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.CAPACITY, source.capacity(), CAPACITY_UNIT);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.AVAILABLE_SPACE, source.freeCapacity(), CAPACITY_UNIT);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.LABEL, source.label(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("Filesystem 매핑에 실패했습니다. mountPointPk={}", source.mountPointPk(), e);
            }
        }
        return mappedData;
    }

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_mountpoint_v2 m
            WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + EXCLUDED_TYPES_SQL + """
            ))
            AND EXISTS (
                SELECT 1 FROM view_device_v2 d
                WHERE d.device_pk = ANY(m.device_fks) AND
            """ + CiSourceFilter.COMPUTER + """
            )
            """;

    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT DISTINCT ON (m.mountpoint_pk)
                m.mountpoint_pk, c.device_pk AS device_fk,
                NULLIF(TRIM(m.mountpoint), '') AS mountpoint,
                NULLIF(TRIM(m.fstype_name), '') AS fstype_name,
                NULLIF(TRIM(m.label), '') AS label,
                m.capacity, m.free_capacity, c.last_discovered
            FROM view_mountpoint_v2 m
            JOIN computer c ON c.device_pk = ANY(m.device_fks)
            WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + EXCLUDED_TYPES_SQL + """
            ))
            ORDER BY m.mountpoint_pk, c.device_pk
            LIMIT %d OFFSET %d
            """;
}
