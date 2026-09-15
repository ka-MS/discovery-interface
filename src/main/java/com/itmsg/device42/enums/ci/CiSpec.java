package com.itmsg.device42.enums.ci;

/** CI 유형별 수집 속성. 유형 enum이 구현한다. */
public interface CiSpec {
    /** Maximo ASSETATTRID. */
    String attributeId();

    /** 분류 템플릿이 없어도 전역 속성 정의로 수집할 때 쓸 표시 순서. 허용하지 않으면 null. */
    default Integer additionalDisplaySequence() {
        return null;
    }

    /** 명시적 추가 속성의 필수 여부. */
    default boolean additionalMandatory() {
        return false;
    }

    /** 원천 단위를 코드로 변환해야 하는 속성. 변환 실패(null)면 해당 속성을 생략한다. */
    default boolean requiresUnit() {
        return false;
    }
}
