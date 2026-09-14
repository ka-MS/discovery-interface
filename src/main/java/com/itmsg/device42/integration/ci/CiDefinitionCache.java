package com.itmsg.device42.integration.ci;

import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.dto.maximo.ci.AssetAttributeDefinition;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.dto.maximo.ci.SpecDefinition;
import com.itmsg.device42.dto.maximo.ci.SpecKey;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** CI 한 번의 실행에서 모든 작업이 공유하는 읽기 전용 정의. DB에 접근하지 않는다. */
public final class CiDefinitionCache {
    private final Map<String, ClassificationDefinition> classifications;
    private final Map<SpecKey, SpecDefinition> specs;
    private final Set<SpecKey> declaredSpecs;
    private final Map<Long, AssetAttributeDefinition> attributes;
    private final Map<String, List<AssetAttributeDefinition>> globalAttributes;

    CiDefinitionCache(Map<String, ClassificationDefinition> classifications,
                      Map<SpecKey, SpecDefinition> specs, Set<SpecKey> declaredSpecs,
                      Map<Long, AssetAttributeDefinition> attributes) {
        this.classifications = Map.copyOf(classifications);
        this.specs = Map.copyOf(specs);
        this.declaredSpecs = Set.copyOf(declaredSpecs);
        this.attributes = Map.copyOf(attributes);
        this.globalAttributes = this.attributes.values().stream()
                .filter(attribute -> attribute.orgId() == null && attribute.siteId() == null)
                .collect(Collectors.groupingBy(AssetAttributeDefinition::attributeId));
    }

    public ClassificationDefinition classification(CiClassification classification) {
        return classifications.get(classification.classificationId());
    }

    public SpecDefinition spec(String classStructureId, String attributeId, String section) {
        return specs.get(new SpecKey(classStructureId, attributeId, section));
    }

    /** 명시적으로 허용한 추가 속성 전용. 기존 템플릿의 설정 오류를 우회하지 않는다. */
    public SpecDefinition additionalSpec(String classStructureId, String attributeId, String section,
                                         int displaySequence, boolean mandatory) {
        if (classifications.values().stream().noneMatch(
                classification -> classification.classStructureId().equals(classStructureId))) {
            return null;
        }
        SpecKey key = new SpecKey(classStructureId, attributeId, section);
        if (declaredSpecs.contains(key)) {
            return specs.get(key);
        }
        List<AssetAttributeDefinition> matches = globalAttributes.getOrDefault(attributeId, List.of());
        if (matches.size() != 1) {
            return null;
        }
        AssetAttributeDefinition attribute = matches.getFirst();
        return new SpecDefinition(null, classStructureId, attributeId, attribute.dataType(),
                section, attribute.measureUnitId(), displaySequence, mandatory, null, null);
    }

    public AssetAttributeDefinition attribute(long id) {
        return attributes.get(id);
    }
}
