package com.itmsg.device42.integration.d42maximo.ci.mapping;

/**
 * NET.IPADDRESS에서 수집할 ASSETATTRID.
 * 대조 기준은 CI 계열의 CI.IPADDRESS 6개다. LABEL만 기준 밖의 의도적 추가다.
 */
public enum IpSpec implements CiSpec {
    DOT_NOTATION("IPADDRESS_DOTNOTATION"),
    STRING_NOTATION("IPADDRESS_STRINGNOTATION"),
    MANAGED_SYSTEM_NAME("IPADDRESS_MANAGEDSYSTEMNAME"),
    /** CI 계열 분류에 MODELOBJECT_ 속성이 없어 승격에서 전달되지 않을 수 있다. */
    LABEL("MODELOBJECT_LABEL");

    private final String attributeId;

    IpSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
