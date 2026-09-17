package com.itmsg.device42.integration.d42maximo.ci.mapping;

/** 전체 ASSETATTRID를 명시한다. 추가 표시 순서가 있는 항목만 템플릿 없이 수집할 수 있다. */
public enum ComputerSpec implements CiSpec {
    NAME("COMPUTERSYSTEM_NAME"),
    SERIAL_NUMBER("COMPUTERSYSTEM_SERIALNUMBER"),
    UUID("COMPUTERSYSTEM_UUID"),
    MODEL("COMPUTERSYSTEM_MODEL"),
    MANUFACTURER("COMPUTERSYSTEM_MANUFACTURER"),
    MEMORY_SIZE("COMPUTERSYSTEM_MEMORYSIZE"),
    CPU_COUNT("COMPUTERSYSTEM_NUMCPUS"),
    CPU_SPEED("COMPUTERSYSTEM_CPUSPEED"),
    CPU_TYPE("COMPUTERSYSTEM_CPUTYPE"),
    ARCHITECTURE("COMPUTERSYSTEM_ARCHITECTURE"),
    PRIMARY_MAC("COMPUTERSYSTEM_PRIMARYMACADDRESS"),
    TYPE("COMPUTERSYSTEM_TYPE"),
    VIRTUAL("COMPUTERSYSTEM_VIRTUAL"),
    VM_ID("COMPUTERSYSTEM_VMID"),
    BIOS_MANUFACTURER("COMPUTERSYSTEM_BIOSMANUFACTURER"),
    BIOS_VERSION("COMPUTERSYSTEM_ROMVERSION"),
    BIOS_RELEASE_DATE("COMPUTERSYSTEM_BIOSRELEASEDATE", 180, false),
    CPU_CORES("COMPUTERSYSTEM_CPUCORESINSTALLED");

    private final String attributeId;
    private final Integer additionalDisplaySequence;
    private final boolean additionalMandatory;

    ComputerSpec(String attributeId) {
        this(attributeId, null, false);
    }

    ComputerSpec(String attributeId, Integer additionalDisplaySequence, boolean additionalMandatory) {
        this.attributeId = attributeId;
        this.additionalDisplaySequence = additionalDisplaySequence;
        this.additionalMandatory = additionalMandatory;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }

    @Override
    public Integer additionalDisplaySequence() {
        return additionalDisplaySequence;
    }

    @Override
    public boolean additionalMandatory() {
        return additionalMandatory;
    }

    @Override
    public boolean requiresUnit() {
        return this == MEMORY_SIZE || this == CPU_SPEED;
    }

    public boolean appliesTo(CiClassification classification) {
        return this != VM_ID || classification == CiClassification.VIRTUAL_COMPUTER;
    }
}
