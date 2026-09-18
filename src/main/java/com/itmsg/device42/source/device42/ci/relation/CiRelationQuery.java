package com.itmsg.device42.source.device42.ci.relation;


import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CiRelationQuery {
    private final DoqlClient doql;
    private final Device42Relation.Selection selection;

    public CiRelationQuery(Device42Relation.Selection selection, DoqlClient doql) {
        this.selection = selection;
        this.doql = doql;
    }

    public long getTotalCount(Device42Relation source) {
        try {
            return doql.query(source.countQuery(selection), rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(source + " 관계 건수 조회에 실패했습니다.", e);
        }
    }

    public List<RelationSource> getData(Device42Relation source, long offset, int limit) {
        String query = source.pageQuery(selection, offset, limit);
        try {
            return doql.query(query, rs -> {
                List<RelationSource> data = new ArrayList<>(limit);
                while (rs.next()) {
                    data.add(new RelationSource(rs.getString("source_pk"), rs.getString("target_pk")));
                }
                return data;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    source + " 관계 조회에 실패했습니다. offset=" + offset, e);
        }
    }
}
