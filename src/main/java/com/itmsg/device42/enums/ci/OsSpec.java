package com.itmsg.device42.enums.ci;

/**
 * SYS.OPERATINGSYSTEM에서 수집할 ASSETATTRID.
 * 대조 기준은 CI 계열의 CI.OS 7개다. KERNEL_ARCHITECTURE만 기준 밖의 의도적 추가다.
 */
public enum OsSpec implements CiSpec {
    OS_NAME("OPERATINGSYSTEM_OSNAME"),
    NAME("OPERATINGSYSTEM_NAME"),
    OS_VERSION("OPERATINGSYSTEM_OSVERSION"),
    KERNEL_VERSION("OPERATINGSYSTEM_KERNELVERSION"),
    KERNEL_ARCHITECTURE("OPERATINGSYSTEM_KERNELARCHITECTURE");

    private final String attributeId;

    OsSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
