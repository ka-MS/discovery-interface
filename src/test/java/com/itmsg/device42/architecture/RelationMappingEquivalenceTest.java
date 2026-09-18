package com.itmsg.device42.architecture;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.MaximoCiIdentity;
import com.itmsg.device42.pipeline.d42maximo.ci.relation.CiRelationMapper;
import com.itmsg.device42.pipeline.d42maximo.ci.relation.CiRelationSource;
import com.itmsg.device42.source.device42.ci.relation.RelationSource;
import com.itmsg.device42.target.maximo.ci.ActCiRelationUpsert;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class RelationMappingEquivalenceTest {
    @Test
    void sevenRelationsMapRawKeysToTheExactBaselineIdentifiersCodesAndPageOrder() throws Exception {
        String[][] prefixes = {{"DEVICEOS", "DEVICE"}, {"DEVICE", "PART"}, {"DEVICE", "MOUNTPOINT"},
                {"DEVICE", "DEVICE"}, {"DATABASEINSTANCE", "DEVICE"}, {"DEVICE", "IPADDRESS"}, {"DEVICE", "DEVICE"}};
        String[] codes = {"RELATION.INSTALLEDON", "RELATION.CONTAINS", "RELATION.CONTAINS", "VIRTUALIZES",
                "RELATION.RUNSON", "USES", "FEDERATES"};
        var mapper = new CiRelationMapper();
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:relation-mapping")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE TABLE pairs(a bigint, b bigint)");
            // 10/2 distinguishes lexical from numeric order; duplicate and fan-out must survive.
            jdbc.execute("INSERT INTO pairs VALUES (2,2),(10,2),(1,10),(1,2),(1,2),(1,3),(9999999999,2),(NULL,2),(2,NULL)");
            for (int i = 0; i < prefixes.length; i++) {
                var relation = CiRelationSource.values()[i];
                var expectedAll = new ArrayList<ActCiRelationUpsert>();
                var actualAll = new ArrayList<ActCiRelationUpsert>();
                for (int offset = 0; offset < 9; offset += 2) {
                    String page = " LIMIT 2 OFFSET " + offset;
                    String code = codes[i];
                    var expected = jdbc.query("SELECT 'D42:" + prefixes[i][0] + ":' || CAST(a AS varchar) AS sourceci, "
                                    + "'D42:" + prefixes[i][1] + ":' || CAST(b AS varchar) AS targetci FROM pairs "
                                    + "ORDER BY sourceci,targetci" + page,
                            (rs, n) -> new ActCiRelationUpsert(rs.getString(1), rs.getString(2), code));
                    var source = jdbc.query("SELECT CAST(a AS varchar) AS source_pk, CAST(b AS varchar) AS target_pk "
                                    + "FROM pairs ORDER BY source_pk,target_pk" + page,
                            (rs, n) -> new RelationSource(rs.getString(1), rs.getString(2)));
                    var actual = mapper.mapData(relation, source);
                    assertThat(actual).as("%s offset %s", relation, offset).isEqualTo(expected);
                    expectedAll.addAll(expected);
                    actualAll.addAll(actual);
                }
                assertThat(actualAll).hasSize(9).isEqualTo(expectedAll);
                assertThat(MaximoCiIdentity.of(relation.source().from(), 10L)).isEqualTo("D42:" + prefixes[i][0] + ":10");
                assertThat(MaximoCiIdentity.of(relation.source().to(), 2L)).isEqualTo("D42:" + prefixes[i][1] + ":2");
            }
        }
    }
}
