package com.itmsg.device42.source.device42.ci.databaseinstance;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseInstanceCiQuery {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInstanceCiQuery.class);

    private final DoqlClient doql;

    public DatabaseInstanceCiQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.query(TOTAL_COUNT_QUERY, rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("DB Instance 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DatabaseInstanceSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try {
            return doql.query(query, rs -> {
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
            });
        } catch (SQLException e) {
            throw new IllegalStateException("DB Instance 조회에 실패했습니다. offset=" + offset, e);
        }
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
