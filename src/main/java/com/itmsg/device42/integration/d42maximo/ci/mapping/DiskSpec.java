package com.itmsg.device42.integration.d42maximo.ci.mapping;

/** DEV.DISKDRIVE에서 수집할 ASSETATTRID. */
public enum DiskSpec implements CiSpec {
    MODEL("MEDIAACCESSDEVICE_MODEL"),
    SERIAL_NUMBER("MEDIAACCESSDEVICE_SERIALNUMBER"),
    DISK_SIZE("DISKDRIVE_DISKSIZE");

    private final String attributeId;

    DiskSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }

    @Override
    public boolean requiresUnit() {
        return this == DISK_SIZE;
    }
}
