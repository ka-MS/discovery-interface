package com.itmsg.device42.enums.ci;

/** Generic Computer System 계열에서만 사용하는 추가 속성. */
public enum GenericComputerSystemSpec implements CiSpec {
    GENERIC_TYPE("GENERICCOMPUTERSYSTEM_GENERICTYPE");

    private final String attributeId;

    GenericComputerSystemSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
