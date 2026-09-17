package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.DatabaseInstanceSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.DatabaseInstanceSpec;
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
public class DatabaseInstanceCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInstanceCiIntegrate.class);
    static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public DatabaseInstanceCiIntegrate(Device42ConnectionFactory connectionFactory, ActCiWriter writer,
                                       CiSpecMapper specMapper) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
        this.specMapper = specMapper;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 DB Instance 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 DB Instance 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("DB Instance 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<DatabaseInstanceSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 DB Instance가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("DB Instance CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DB Instance 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DatabaseInstanceSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<DatabaseInstanceSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long databaseInstancePk = rs.getLong("databaseinstance_pk");
                try {
                    data.add(new DatabaseInstanceSource(
                            databaseInstancePk, rs.getString("dbinstance_name"),
                            rs.getString("database_type"), rs.getString("resource_identifier"),
                            rs.getString("version_text"), rs.getString("source_description"),
                            rs.getString("install_path"), rs.getString("last_changed")));
                } catch (SQLException e) {
                    log.error("DB Instance 원천 변환에 실패했습니다. databaseInstancePk={}", databaseInstancePk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("DB Instance 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<DatabaseInstanceSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (DatabaseInstanceSource source : data) {
            try {
                CiClassification classification = selectClassification(source.engine());
                ClassificationDefinition definition = definitions.classification(classification);
                if (definition == null) {
                    log.warn("DB Instance 분류가 없어 건너뜁니다. databaseInstancePk={}, classification={}",
                            source.databaseInstancePk(), classification);
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:DATABASEINSTANCE:" + source.databaseInstancePk(), source.name(),
                        definition.classStructureId(), source.description(),
                        SourceTimestamp.toLocalDateTime(source.lastChanged()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.NAME, source.name(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.PRODUCT_NAME, source.engine(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.PRODUCT_VERSION,
                        source.versionText(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.KEY_NAME, source.identifier(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.HOME, source.installPath(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("DB Instance 매핑에 실패했습니다. databaseInstancePk={}",
                        source.databaseInstancePk(), e);
            }
        }
        return mappedData;
    }

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
    }

    /**
     * 엔진 표기로 분류를 고른다. 전용 분류가 없는 엔진과 표기 없는 행은 범용 분류로 보낸다.
     * 엔진 문자열은 분류와 별개로 APPSERVER_PRODUCTNAME에 그대로 남는다.
     */
    static CiClassification selectClassification(String engine) {
        return switch (engine == null ? "" : engine.trim()) {
            case "Microsoft SQL" -> CiClassification.SQL_SERVER;
            case "DB2" -> CiClassification.DB2_INSTANCE;
            case "Oracle Database" -> CiClassification.ORACLE_INSTANCE;
            default -> CiClassification.DATABASE_SERVER;
        };
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*) FROM view_databaseinstance_v2 i
            """;

    /**
     * Instance 본체는 Device 수집 범위로 걸러내지 않는다. Component·Device가 없는 Instance도 보존한다.
     * 스캔 시각은 r.last_discovered가 전건 NULL이라 r.last_changed를 쓴다.
     * notes는 빈 문자열과 NULL을 구분해 그대로 넘긴다.
     */
    private static final String SOURCE_QUERY = """
            SELECT i.databaseinstance_pk,
                NULLIF(TRIM(i.dbinstance_name), '') AS dbinstance_name,
                NULLIF(TRIM(i.database_type), '') AS database_type,
                NULLIF(TRIM(r.identifier), '') AS resource_identifier,
                NULLIF(TRIM(CAST(r.details AS JSONB)->>'version'), '') AS version_text,
                r.notes AS source_description,
                r.last_changed,
                NULLIF(TRIM(CAST(a.json AS JSONB)->'products'->0->>'install_path'), '') AS install_path
            FROM view_databaseinstance_v2 i
            LEFT JOIN view_resource_v2 r ON r.resource_pk = i.databaseinstance_pk
            LEFT JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
            ORDER BY i.databaseinstance_pk
            LIMIT %d OFFSET %d
            """;
}
