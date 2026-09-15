package com.itmsg.device42.enums.ci;

/**
 * SYS.FILESYSTEM에서 수집할 ASSETATTRID.
 * 대조 기준은 CI 계열의 CI.FILESYSTEM 16개다. LABEL만 기준 밖의 의도적 추가다.
 */
public enum FilesystemSpec implements CiSpec {
    MOUNT_POINT("FILESYSTEM_MOUNTPOINT"),
    TYPE("FILESYSTEM_TYPE"),
    CAPACITY("FILESYSTEM_CAPACITY"),
    AVAILABLE_SPACE("FILESYSTEM_AVAILABLESPACE"),
    /** CI 계열 분류에 MODELOBJECT_ 속성이 없어 승격에서 전달되지 않을 수 있다. */
    LABEL("MODELOBJECT_LABEL");

    private final String attributeId;

    FilesystemSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
