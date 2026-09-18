package com.itmsg.device42.source.device42.software.catalog;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SoftwareCatalogQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;
    private final String supplementalName;

    public SoftwareCatalogQuery(DeviceSelection selection, String supplementalName, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
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
            throw new IllegalStateException("TLOAM 소프트웨어 카탈로그 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<SoftwareProductSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<SoftwareProductSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new SoftwareProductSource(
                            resultSet.getString("software_name"),
                            resultSet.getString("version"),
                            resultSet.getString("manufacturer")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "TLOAM 소프트웨어 카탈로그 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql())
                .replace("{{SUPPLEMENTAL_NAME}}", DeviceSelection.literal(supplementalName));
    }

    private static final String DEVICE_FILTER = "{{DEVICE_FILTER}}";

    private static final String CATALOG_SOURCE_QUERY = """
            SELECT DISTINCT
                COALESCE(NULLIF(TRIM(s.name), ''), {{SUPPLEMENTAL_NAME}}) AS software_name,
                NULLIF(TRIM(u.version), '') AS version,
                COALESCE(NULLIF(TRIM(v.name), ''), {{SUPPLEMENTAL_NAME}}) AS manufacturer
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

    // 동일 그룹의 대표 원천값은 NULL로 돌려준다. 타겟 기본값 생성은 Mapper가 소유한다.
    // 내부 그룹 키는 기존 DISTINCT/정렬/페이지 경계 보존을 위해 호출자의 동치값 정책을 사용한다.
    private static final String SOURCE_QUERY = """
            SELECT NULLIF(catalog.software_name, {{SUPPLEMENTAL_NAME}}) AS software_name,
                catalog.version,
                NULLIF(catalog.manufacturer, {{SUPPLEMENTAL_NAME}}) AS manufacturer
            FROM (
            """ + CATALOG_SOURCE_QUERY + """
            ) catalog
            ORDER BY catalog.software_name, catalog.version, catalog.manufacturer
            """;
}
