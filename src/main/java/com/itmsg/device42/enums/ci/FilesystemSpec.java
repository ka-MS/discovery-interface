package com.itmsg.device42.enums.ci;

/** SYS.FILESYSTEM에서 수집할 ASSETATTRID. */
public enum FilesystemSpec implements CiSpec {
    MOUNT_POINT("FILESYSTEM_MOUNTPOINT"),
    TYPE("FILESYSTEM_TYPE"),
    CAPACITY("FILESYSTEM_CAPACITY"),
    AVAILABLE_SPACE("FILESYSTEM_AVAILABLESPACE");

    private final String attributeId;

    FilesystemSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
