package com.itmsg.device42.pipeline.d42maximo.ci.relation;

import com.itmsg.device42.source.device42.ci.relation.Device42Relation;

/** Maximo 관계 방향·코드와 D42 연결 사실의 대응. */
public enum CiRelationSource {
    OS_INSTALLED_ON_COMPUTER(Device42Relation.OS_DEVICE, "RELATION.INSTALLEDON"),
    COMPUTER_CONTAINS_DISK(Device42Relation.DEVICE_DISK, "RELATION.CONTAINS"),
    COMPUTER_CONTAINS_FILESYSTEM(Device42Relation.DEVICE_FILESYSTEM, "RELATION.CONTAINS"),
    HOST_VIRTUALIZES_VM(Device42Relation.HOST_VM, "VIRTUALIZES"),
    DB_INSTANCE_RUNS_ON_DEVICE(Device42Relation.DATABASE_INSTANCE_DEVICE, "RELATION.RUNSON"),
    DEVICE_USES_IP(Device42Relation.DEVICE_IP, "USES"),
    NETWORK_CLUSTER_FEDERATES_DEVICE(Device42Relation.NETWORK_CLUSTER_DEVICE, "FEDERATES");

    private final Device42Relation source;
    private final String relationNum;

    CiRelationSource(Device42Relation source, String relationNum) {
        this.source = source;
        this.relationNum = relationNum;
    }

    public Device42Relation source() { return source; }
    public String relationNum() { return relationNum; }
}
