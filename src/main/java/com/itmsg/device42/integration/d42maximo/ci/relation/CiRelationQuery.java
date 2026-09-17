package com.itmsg.device42.integration.d42maximo.ci.relation;

import com.itmsg.device42.device42.DoqlClient;
import com.itmsg.device42.maximo.ci.ActCiRelationUpsert;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CiRelationQuery {
    private final DoqlClient doql;

    public CiRelationQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount(CiRelationSource source) {
        try {
            return doql.query(source.countQuery(), rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(source + " 관계 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ActCiRelationUpsert> getData(CiRelationSource source, long offset, int limit) {
        String query = source.pageQuery(offset, limit);
        try {
            return doql.query(query, rs -> {
                List<ActCiRelationUpsert> data = new ArrayList<>(limit);
                while (rs.next()) {
                    data.add(new ActCiRelationUpsert(
                            rs.getString("sourceci"), rs.getString("targetci"), source.relationNum()));
                }
                return data;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    source + " 관계 조회에 실패했습니다. offset=" + offset, e);
        }
    }
}
