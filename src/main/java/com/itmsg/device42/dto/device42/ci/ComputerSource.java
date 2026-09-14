package com.itmsg.device42.dto.device42.ci;

import java.math.BigDecimal;

/** Computer 한 대의 본체·스펙 원천. 물리·가상이 같은 조회 구조를 사용한다. */
public record ComputerSource(
        long devicePk, String type, String name, String notes, String serialNo,
        String uuid, String lastDiscovered, String model, String manufacturer,
        BigDecimal ram, String ramUnit, Integer totalCpus, Integer corePerCpu,
        BigDecimal cpuSpeed, String cpuSpeedUnit, String cpuType, String architecture,
        String primaryMac, String vmId, String biosManufacturer, String biosVersion,
        String biosReleaseDate
) {
}
