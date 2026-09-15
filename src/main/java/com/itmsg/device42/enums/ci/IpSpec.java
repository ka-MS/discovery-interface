package com.itmsg.device42.enums.ci;

/**
 * NET.IPADDRESS에서 수집할 ASSETATTRID.
 * 대조 기준은 CI 계열의 CI.IPADDRESS 6개다. 원천 대응이 있는 주소 표기 둘을 채택했다.
 */
public enum IpSpec implements CiSpec {
    DOT_NOTATION("IPADDRESS_DOTNOTATION"),
    STRING_NOTATION("IPADDRESS_STRINGNOTATION");

    private final String attributeId;

    IpSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
