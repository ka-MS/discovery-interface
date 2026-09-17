package com.itmsg.device42.integration.d42maximo.ci.ip;

import com.itmsg.device42.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IpCiQuery {

    private static final Logger log = LoggerFactory.getLogger(IpCiQuery.class);

    private final DoqlClient doql;

    public IpCiQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.query(TOTAL_COUNT_QUERY, rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("IP 건수 조회에 실패했습니다.", e);
        }
    }

    public List<IpSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try {
            return doql.query(query, rs -> {
                List<IpSource> data = new ArrayList<>(limit);
                while (rs.next()) {
                    long ipAddressPk = rs.getLong("ipaddress_pk");
                    try {
                        data.add(new IpSource(
                                ipAddressPk, rs.getLong("device_fk"), rs.getString("ip_address"),
                                rs.getString("device_name"), rs.getString("label"),
                                rs.getString("notes"), rs.getString("last_discovered")));
                    } catch (SQLException e) {
                        log.error("IP 원천 변환에 실패했습니다. ipAddressPk={}", ipAddressPk, e);
                    }
                }
                return data;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("IP 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    /**
     * IP는 장비에 종속된 개체가 아니라 독립 CI이므로 부모 유형으로 좁히지 않는다.
     * 장비에 연결된 주소를 모두 가져오며, 서브넷에만 할당된 주소는 제외한다.
     * 장비 유형을 추가해도 이 조회를 고치지 않는다.
     */
    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_ipaddress_v2 i
            WHERE EXISTS (
                SELECT 1 FROM view_device_v2 d WHERE d.device_pk = ANY(i.device_fks)
            )
            """;

    /**
     * device_fks가 최대 3~7개라 조인이 같은 주소를 여러 행으로 만든다. DISTINCT ON으로 하나만 남긴다.
     * 여러 장비에 걸린 주소는 device_pk가 가장 작은 장비의 이름을 MANAGEDSYSTEMNAME에 쓴다.
     */
    private static final String SOURCE_QUERY = """
            SELECT DISTINCT ON (i.ipaddress_pk)
                i.ipaddress_pk, d.device_pk AS device_fk,
                HOST(i.ip_address) AS ip_address,
                NULLIF(TRIM(d.name), '') AS device_name,
                NULLIF(TRIM(i.label), '') AS label,
                NULLIF(TRIM(i.notes), '') AS notes,
                i.last_discovered
            FROM view_ipaddress_v2 i
            JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
            ORDER BY i.ipaddress_pk, d.device_pk
            LIMIT %d OFFSET %d
            """;
}
