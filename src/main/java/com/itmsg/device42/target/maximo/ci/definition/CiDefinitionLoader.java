package com.itmsg.device42.target.maximo.ci.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class CiDefinitionLoader {
    private static final Logger log = LoggerFactory.getLogger(CiDefinitionLoader.class);
    private final NamedParameterJdbcTemplate jdbc;

    public CiDefinitionLoader(@Qualifier("maximoJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public CiDefinitionCache load(Collection<String> classificationIds) {
        var names = classificationIds.stream().distinct().toList();
        Map<String, ClassificationDefinition> classifications = new HashMap<>();
        jdbc.query(CLASS_QUERY, Map.of("classifications", names), rs -> {
            String name = rs.getString("CLASSIFICATIONID");
            if (classifications.putIfAbsent(name,
                    new ClassificationDefinition(name, rs.getString("CLASSSTRUCTUREID"))) != null) {
                throw new IllegalStateException("ACTCI 분류가 중복됩니다: " + name);
            }
        });

        Map<Long, AssetAttributeDefinition> attributes = new HashMap<>();
        jdbc.query(ATTRIBUTE_QUERY, Map.of(), rs -> {
            long id = rs.getLong("ASSETATTRIBUTEID");
            attributes.put(id, new AssetAttributeDefinition(id, rs.getString("ASSETATTRID"),
                    rs.getString("DATATYPE"), rs.getString("MEASUREUNITID"),
                    rs.getString("ORGID"), rs.getString("SITEID")));
        });

        Map<SpecKey, SpecDefinition> specs = new HashMap<>();
        Set<SpecKey> declaredSpecs = new HashSet<>();
        if (classifications.isEmpty()) {
            log.warn("ACTCI에 적용된 분류가 없습니다. 모든 CI가 제외됩니다. 조회한 분류={}", names);
        } else {
            var classIds = classifications.values().stream()
                    .map(ClassificationDefinition::classStructureId).toList();
            jdbc.query(SPEC_QUERY, Map.of("classIds", classIds), rs -> {
                SpecKey key = new SpecKey(rs.getString("CLASSSTRUCTUREID"),
                        rs.getString("ASSETATTRID"), rs.getString("SECTION"));
                if (!declaredSpecs.add(key)) {
                    throw new IllegalStateException("ACTCI 스펙 템플릿이 중복됩니다: " + key);
                }

                AssetAttributeDefinition attribute = attributes.get(rs.getLong("ASSETATTRIBUTEID"));
                Integer sequence = rs.getObject("SEQUENCE", Integer.class);
                Integer mandatory = rs.getObject("MANDATORY", Integer.class);
                if (attribute == null || !Objects.equals(attribute.attributeId(), key.attributeId())
                        || sequence == null || mandatory == null
                        || sequence < Short.MIN_VALUE || sequence > Short.MAX_VALUE
                        || (mandatory != 0 && mandatory != 1)) {
                    log.warn("속성 정의 또는 ACTCI 적용 설정이 없어 스펙을 제외합니다. key={}", key);
                    return;
                }
                specs.put(key, new SpecDefinition(
                        rs.getLong("CLASSSPECID"), key.classStructureId(), key.attributeId(),
                        attribute.dataType(), key.section(), rs.getString("MEASUREUNITID"),
                        sequence, mandatory == 1, rs.getString("LINKEDTOATTRIBUTE"), rs.getString("LINKEDTOSECTION")));
            });
        }
        log.info("CI 정의 캐시를 준비했습니다. classifications={}, specs={}, attributes={}",
                classifications.size(), specs.size(), attributes.size());
        return new CiDefinitionCache(classifications, specs, declaredSpecs, attributes);
    }

    private static final String CLASS_QUERY = """
            SELECT s.CLASSIFICATIONID,s.CLASSSTRUCTUREID
            FROM MAXIMO.CLASSSTRUCTURE s
            WHERE s.CLASSIFICATIONID IN (:classifications)
              AND EXISTS (
                  SELECT 1 FROM MAXIMO.CLASSUSEWITH u
                  WHERE u.CLASSSTRUCTUREID=s.CLASSSTRUCTUREID AND u.OBJECTNAME='ACTCI'
              )
            """;

    private static final String ATTRIBUTE_QUERY = """
            SELECT ASSETATTRIBUTEID,ASSETATTRID,DATATYPE,MEASUREUNITID,ORGID,SITEID
            FROM MAXIMO.ASSETATTRIBUTE
            """;

    private static final String SPEC_QUERY = """
            SELECT c.CLASSSTRUCTUREID,c.CLASSSPECID,c.ASSETATTRID,c.ASSETATTRIBUTEID,
                c.SECTION,c.MEASUREUNITID,c.LINKEDTOATTRIBUTE,c.LINKEDTOSECTION,
                u.SEQUENCE,u.MANDATORY
            FROM MAXIMO.CLASSSPEC c
            LEFT JOIN MAXIMO.CLASSSPECUSEWITH u
              ON u.CLASSSPECID=c.CLASSSPECID AND u.OBJECTNAME='ACTCI' AND u.USEINSPEC=1
              AND u.CLASSSTRUCTUREID=c.CLASSSTRUCTUREID AND u.ASSETATTRID=c.ASSETATTRID
              AND (u.SECTION=c.SECTION OR (u.SECTION IS NULL AND c.SECTION IS NULL))
              AND u.ORGID IS NULL AND u.SITEID IS NULL
            WHERE c.CLASSSTRUCTUREID IN (:classIds)
              AND c.ORGID IS NULL AND c.SITEID IS NULL
            """;
}
