package com.itmsg.device42.architecture;

import com.itmsg.device42.pipeline.d42maximo.MaximoQueries;
import com.itmsg.device42.pipeline.d42maximo.selection.MaximoSourcePolicy;
import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.source.device42.ci.relation.Device42Relation;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 27개 본체/기준정보 조회와 7개 관계 SQL의 전체 범위를 기준 커밋과 대조한다. */
class SourceSqlParityTest {
    @Test
    void everyEffectiveCountAndPageQueryPreservesBaselineExceptExplicitlyMovedProjection() throws Exception {
        var executed = new ArrayList<String>();
        var doql = mock(DoqlClient.class);
        var reader = (org.mockito.stubbing.Answer<Object>) invocation -> {
            executed.add(invocation.getArgument(0));
            return invocation.getArgument(1, DoqlClient.ResultReader.class).read(mock(ResultSet.class));
        };
        when(doql.query(anyString(), any())).thenAnswer(reader);
        when(doql.preparedQuery(anyString(), any())).thenAnswer(reader);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DoqlClient.class, () -> doql);
            context.register(MaximoQueries.class);
            context.refresh();
            int checked = 0;
            for (var method : MaximoQueries.class.getDeclaredMethods()) {
                var type = method.getReturnType();
                if (type.getSimpleName().equals("CiRelationQuery")) continue;
                var query = context.getBean(type);
                var baseline = Class.forName(BaselineSql.class.getName() + "$" + type.getSimpleName());
                executed.clear();
                type.getMethod("getTotalCount").invoke(query);
                type.getMethod("getData", long.class, int.class).invoke(query, 20L, 10);
                String count = field(baseline, "TOTAL_COUNT_QUERY", "DEVICE_TOTAL_COUNT_QUERY");
                String page = field(baseline, "SOURCE_QUERY", "DEVICE_QUERY");
                page = page.contains("%d") ? page.formatted(10, 20) : page + "LIMIT 10 OFFSET 20";
                // 세 컬럼은 기존 ResultSet reader가 읽지 않으며 Mapper가 이미 계산하던 타겟 표현이다.
                if (type.getSimpleName().equals("DeviceCiQuery")) {
                    page = page.replaceAll("(?m)^.*(?:AS source_id,|AS system_type,|AS is_virtual,)\\n", "");
                }
                if (type.getSimpleName().equals("NetDeviceQuery")) {
                    page = page.replace("FROM view_device_v2\n", "FROM view_device_v2 d\n")
                            .replace("WHERE network_device = true", "WHERE d.network_device = true")
                            .replace("AND type = 'physical'", "AND d.type = 'physical'");
                }
                assertThat(executed).as(type.getName()).hasSize(2);
                assertThat(canonical(executed.get(0))).as("%s COUNT", type.getSimpleName()).isEqualTo(canonical(count));
                assertThat(canonical(executed.get(1))).as("%s PAGE", type.getSimpleName()).isEqualTo(canonical(page));
                assertThat(executed).allSatisfy(sql -> assertThat(sql).doesNotContain("{{", "D42:", "RELATION."));
                checked++;
            }
            assertThat(checked).isEqualTo(27);
        }
    }

    @Test
    void everyRelationPreservesJoinsFiltersMultiplicityAndTextualPkOrdering() {
        var baseline = BaselineSql.CiRelationSource.values();
        var current = Device42Relation.values();
        assertThat(current).hasSameSizeAs(baseline);
        for (int i = 0; i < current.length; i++) {
            assertThat(canonical(current[i].countQuery(MaximoSourcePolicy.RELATIONS)))
                    .as("%s count", current[i]).isEqualTo(canonical(baseline[i].countQuery()));
            String expected = baseline[i].pageQuery(20, 10)
                    .replaceAll("'D42:[A-Z]+:' \\|\\| ", "")
                    .replace("sourceci", "source_pk").replace("targetci", "target_pk");
            assertThat(canonical(current[i].pageQuery(MaximoSourcePolicy.RELATIONS, 20, 10)))
                    .as("%s page", current[i]).isEqualTo(canonical(expected));
        }
    }

    private static String field(Class<?> type, String... names) throws Exception {
        for (String name : names) {
            try {
                return (String) type.getDeclaredField(name).get(null);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new AssertionError("기준 SQL 없음: " + type);
    }

    private static String compact(String sql) {
        return sql.replaceAll("\\s+", " ").replaceAll("\\s*([(),])\\s*", "$1").trim();
    }

    private static String canonical(String sql) {
        String value = compact(sql);
        // 정책은 아래 동등성 테스트가 실행 검증한다. 여기는 SQL 본체의 모든 나머지 토큰을 대조한다.
        String[][] scopes = {
                {BaselineSql.CiSourceFilter.DEVICE, MaximoSourcePolicy.CI_DEVICE.sql(), "<ci-device>"},
                {BaselineSql.CiSourceFilter.COMPUTER, MaximoSourcePolicy.CI_COMPUTER.sql(), "<ci-computer>"},
                {BaselineSql.CpuQuery.DEVICE_FILTER, MaximoSourcePolicy.ASSET_COMPUTER.sql(), "<asset-computer>"},
                {BaselineSql.DeployedAssetQuery.DEVICE_FILTER, MaximoSourcePolicy.ASSET_DEVICE.sql(), "<asset-device>"},
                {BaselineSql.DpamManufacturerQuery.PARENT_FILTER, MaximoSourcePolicy.ASSET_DEVICE.sql(), "<asset-device>"},
                {"d.type IN ('virtual', 'physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15) "
                        + "AND (d.network_device = false OR d.network_device IS NULL) AND d.physicalsubtype = 'Network Printer'",
                        MaximoSourcePolicy.ASSET_PRINTER.sql(), "<printer>"},
                {"d.network_device = true AND d.type = 'physical'", MaximoSourcePolicy.ASSET_NETWORK.sql(), "<network>"}
        };
        for (String[] scope : scopes) {
            value = value.replace(compact(scope[0]), scope[2]).replace(compact(scope[1]), scope[2]);
        }
        return value.replaceAll("\\s*(<[^>]+>)\\s*", " $1 ").trim();
    }
}
