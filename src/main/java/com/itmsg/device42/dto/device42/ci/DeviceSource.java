package com.itmsg.device42.dto.device42.ci;

import java.math.BigDecimal;

/** Device 한 대의 본체·스펙과 분류 진단 원천. */
public record DeviceSource(
        long devicePk, String type, String physicalSubtype, Boolean networkDevice,
        Long clusterPk, String networkKind, Integer networkKindCount, Integer clusterCount,
        String name, String notes, String serialNo,
        String uuid, String lastDiscovered, String model, String manufacturer,
        BigDecimal ram, String ramUnit, Integer totalCpus, Integer corePerCpu,
        BigDecimal cpuSpeed, String cpuSpeedUnit, String cpuType, String architecture,
        String primaryMac, String vmId, String biosManufacturer, String biosVersion,
        String biosReleaseDate
) {
}
