package com.itmsg.device42.enums.ci;

/** NET.IPADDRESS에서 수집할 ASSETATTRID. 원천 대응이 주소 하나뿐이다. */
public enum IpSpec implements CiSpec {
    DOT_NOTATION("IPADDRESS_DOTNOTATION");

    private final String attributeId;

    IpSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
