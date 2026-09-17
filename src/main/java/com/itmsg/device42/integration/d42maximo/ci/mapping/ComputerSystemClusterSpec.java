package com.itmsg.device42.integration.d42maximo.ci.mapping;

/** 논리 Network Cluster에서 의미가 확인된 최소 속성. */
public enum ComputerSystemClusterSpec implements CiSpec {
    MANAGED_SYSTEM_NAME("COMPUTERSYSTEMCLUSTER_MANAGEDSYSTEMNAME"),
    LOCATION_TAG("COMPUTERSYSTEMCLUSTER_LOCATIONTAG");

    private final String attributeId;

    ComputerSystemClusterSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
