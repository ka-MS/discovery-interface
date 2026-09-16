package com.itmsg.device42.integration.ci;

import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.SpecDefinition;
import com.itmsg.device42.enums.ci.CiSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 정의 캐시의 템플릿으로 ACTCISPEC 한 건을 만든다. 모든 CI 유형이 공유한다. */
@Component
public class CiSpecMapper {
    private static final Logger log = LoggerFactory.getLogger(CiSpecMapper.class);

    /** 정의·자료형이 맞으면 specs에 추가한다. 정규 템플릿은 값이 없어도 NULL 행을 만든다. */
    public void addSpec(List<ActCiSpecUpsert> specs, CiDefinitionCache definitions,
                        ActCiUpsert parent, CiSpec field, Object value, String unit) {
        boolean hasValue = value != null && (!(value instanceof String text) || !text.isBlank());
        SpecDefinition template = definitions.spec(parent.classStructureId(), field.attributeId(), null);
        if (template == null && hasValue && field.additionalDisplaySequence() != null) {
            template = definitions.additionalSpec(parent.classStructureId(), field.attributeId(), null,
                    field.additionalDisplaySequence(), field.additionalMandatory());
        }
        if (template == null) {
            if (hasValue) {
                log.warn("스펙 정의가 없어 건너뜁니다. actCiNum={}, attribute={}",
                        parent.actCiNum(), field.attributeId());
            }
            return;
        }
        if (hasValue && field.requiresUnit() && unit == null) {
            log.warn("단위 코드가 없어 스펙을 건너뜁니다. actCiNum={}, attribute={}", parent.actCiNum(), template.assetAttrId());
            return;
        }
        String text = null;
        BigDecimal number = null;
        if (hasValue) {
            if ("ALN".equals(template.dataType()) && value instanceof String string) {
                text = string;
            } else if ("NUMERIC".equals(template.dataType()) && value instanceof BigDecimal decimal) {
                number = decimal;
            } else {
                log.warn("자료형이 맞지 않아 스펙을 건너뜁니다. actCiNum={}, attribute={}",
                        parent.actCiNum(), template.assetAttrId());
                return;
            }
        }
        specs.add(new ActCiSpecUpsert(
                parent.actCiNum(), parent.classStructureId(), template.assetAttrId(),
                template.classSpecId(), template.section(), template.displaySequence(), template.mandatory(),
                unit == null ? template.measureUnitId() : unit,
                template.linkedToAttribute(), template.linkedToSection(), text, number,
                parent.changeBy(), parent.changeDate()));
    }
}
