package com.itmsg.device42.dto.maximo.ci;

/** 같은 속성도 분류와 섹션에 따라 다른 템플릿이다. NULL 섹션을 그대로 보존한다. */
public record SpecKey(String classStructureId, String attributeId, String section) {
}
