package com.itmsg.device42.pipeline.d42maximo.ci.relation;

import com.itmsg.device42.source.device42.ci.relation.RelationSource;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.MaximoCiIdentity;
import com.itmsg.device42.target.maximo.ci.ActCiRelationUpsert;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CiRelationMapper {
    public List<ActCiRelationUpsert> mapData(CiRelationSource relation, List<RelationSource> rows) {
        return rows.stream().map(row -> new ActCiRelationUpsert(
                MaximoCiIdentity.of(relation.source().from(), row.fromPk()),
                MaximoCiIdentity.of(relation.source().to(), row.toPk()), relation.relationNum())).toList();
    }
}
