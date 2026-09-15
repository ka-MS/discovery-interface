package com.itmsg.device42.enums.ci;

/** SYS.OPERATINGSYSTEM에서 수집할 ASSETATTRID. */
public enum OsSpec implements CiSpec {
    OS_NAME("OPERATINGSYSTEM_OSNAME"),
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
