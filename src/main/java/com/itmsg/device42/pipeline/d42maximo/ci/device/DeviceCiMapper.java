package com.itmsg.device42.pipeline.d42maximo.ci.device;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.MaximoCiIdentity;
import com.itmsg.device42.source.device42.ci.relation.Device42Entity;

import com.itmsg.device42.source.device42.ci.device.DeviceSource;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiSpec;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.ComputerSpec;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.ComputerSystemClusterSpec;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.GenericComputerSystemSpec;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.SourceTimestamp;
import com.itmsg.device42.target.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.target.maximo.ci.ActCiUpsert;
import com.itmsg.device42.target.maximo.ci.CiUpsert;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.target.maximo.ci.definition.ClassificationDefinition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeviceCiMapper {

    private static final Logger log = LoggerFactory.getLogger(DeviceCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    private final CiSpecMapper specMapper;

    public DeviceCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    public List<CiUpsert> mapData(List<DeviceSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());
        int printerCount = 0;
        int routerCount = 0;
        int unresolvedNetworkCount = 0;
        int unsupportedCount = 0;

        for (DeviceSource source : data) {
            try {
                CiClassification classification = selectClassification(source);
                if (classification == null) {
                    if ("Network Printer".equals(source.physicalSubtype())) {
                        printerCount++;
                    } else if ("physical".equals(source.type())
                            && Boolean.TRUE.equals(source.networkDevice())) {
                        if ("Router".equals(source.networkKind())) {
                            routerCount++;
                        } else {
                            unresolvedNetworkCount++;
                        }
                    } else {
                        unsupportedCount++;
                    }
                    continue;
                }
                ClassificationDefinition definition = definitions.classification(classification.classificationId());
                if (definition == null) {
                    log.warn("Device 분류가 없어 건너뜁니다. devicePk={}, classification={}",
                            source.devicePk(), classification);
                    continue;
                }

                ActCiUpsert actCi = mapActCi(source, definition, applyDateTime);
                List<ActCiSpecUpsert> specs = mapActCiSpecs(source, actCi, definitions, classification);
                mappedData.add(new CiUpsert(actCi, specs));
            } catch (RuntimeException e) {
                log.error("Device 매핑에 실패했습니다. devicePk={}", source.devicePk(), e);
            }
        }
        if (printerCount + routerCount + unresolvedNetworkCount + unsupportedCount > 0) {
            log.warn("Device 분류 제외 건수. printer={}, router={}, unresolvedNetwork={}, unsupported={}",
                    printerCount, routerCount, unresolvedNetworkCount, unsupportedCount);
        }
        return mappedData;
    }

    private CiClassification selectClassification(DeviceSource source) {
        if ("Network Printer".equals(source.physicalSubtype())) {
            log.warn("물리 Printer 분류가 미정이라 건너뜁니다. devicePk={}", source.devicePk());
            return null;
        }
        if ("cluster".equals(source.type()) && Boolean.TRUE.equals(source.networkDevice())) {
            if (Integer.valueOf(1).equals(source.networkKindCount())
                    && "Switch".equals(source.networkKind())) {
                return CiClassification.COMPUTER_SYSTEM_CLUSTER;
            }
            log.warn("네트워크 Cluster 종류를 판정할 수 없어 건너뜁니다. devicePk={}, "
                            + "networkKind={}, networkKindCount={}",
                    source.devicePk(), source.networkKind(), source.networkKindCount());
            return null;
        }
        if ("virtual".equals(source.type()) && !Boolean.TRUE.equals(source.networkDevice())) {
            return CiClassification.VIRTUAL_COMPUTER;
        }
        if ("physical".equals(source.type()) && Boolean.TRUE.equals(source.networkDevice())) {
            if (Integer.valueOf(1).equals(source.clusterCount())
                    && Integer.valueOf(1).equals(source.networkKindCount())
                    && "Switch".equals(source.networkKind())) {
                return CiClassification.GENERIC_SWITCH;
            }
            log.warn("네트워크 종류를 판정할 수 없어 건너뜁니다. devicePk={}, clusterPk={}, "
                            + "clusterCount={}, networkKind={}, networkKindCount={}",
                    source.devicePk(), source.clusterPk(), source.clusterCount(),
                    source.networkKind(), source.networkKindCount());
            return null;
        }
        if ("physical".equals(source.type())) {
            return CiClassification.COMPUTER;
        }
        log.warn("지원하지 않는 Device 유형이라 건너뜁니다. devicePk={}, type={}, physicalSubtype={}",
                source.devicePk(), source.type(), source.physicalSubtype());
        return null;
    }

    private ActCiUpsert mapActCi(DeviceSource source, ClassificationDefinition definition,
                                 LocalDateTime applyDateTime) {
        LocalDateTime lastScan = SourceTimestamp.toLocalDateTime(source.lastDiscovered());
        return new ActCiUpsert(
                MaximoCiIdentity.of(Device42Entity.DEVICE, source.devicePk()), source.name(), definition.classStructureId(),
                source.notes(), lastScan, CHANGE_BY, applyDateTime, LANG_CODE);
    }

    private List<ActCiSpecUpsert> mapActCiSpecs(DeviceSource source,
                                                ActCiUpsert parent, CiDefinitionCache definitions,
                                                CiClassification classification) {
        List<ActCiSpecUpsert> specs = new ArrayList<>();
        if (classification == CiClassification.COMPUTER_SYSTEM_CLUSTER) {
            addSpec(specs, definitions, parent, ComputerSystemClusterSpec.MANAGED_SYSTEM_NAME,
                    source.name(), null);
            addSpec(specs, definitions, parent, ComputerSystemClusterSpec.LOCATION_TAG,
                    source.snmpLocation(), null);
            return List.copyOf(specs);
        }
        addSpec(specs, definitions, parent, ComputerSpec.NAME, source.name(), null);
        addSpec(specs, definitions, parent, ComputerSpec.SERIAL_NUMBER, source.serialNo(), null);
        addSpec(specs, definitions, parent, ComputerSpec.UUID, source.uuid(), null);
        addSpec(specs, definitions, parent, ComputerSpec.MODEL, source.model(), null);
        addSpec(specs, definitions, parent, ComputerSpec.MANUFACTURER, source.manufacturer(), null);
        addSpec(specs, definitions, parent, ComputerSpec.MEMORY_SIZE, source.ram(),
                source.ram() == null ? null : memoryUnit(source.ramUnit()));
        addSpec(specs, definitions, parent, ComputerSpec.CPU_COUNT, decimal(source.totalCpus()), null);
        addSpec(specs, definitions, parent, ComputerSpec.CPU_SPEED, source.cpuSpeed(),
                source.cpuSpeed() == null ? null : speedUnit(source.cpuSpeedUnit()));
        addSpec(specs, definitions, parent, ComputerSpec.CPU_TYPE, source.cpuType(), null);
        addSpec(specs, definitions, parent, ComputerSpec.ARCHITECTURE, source.architecture(), null);
        addSpec(specs, definitions, parent, ComputerSpec.PRIMARY_MAC, source.primaryMac(), null);
        addSpec(specs, definitions, parent, ComputerSpec.TYPE, "ComputerSystem", null);
        addSpec(specs, definitions, parent, ComputerSpec.VIRTUAL,
                Boolean.toString("virtual".equals(source.type())), null);
        if (ComputerSpec.VM_ID.appliesTo(classification)) {
            addSpec(specs, definitions, parent, ComputerSpec.VM_ID, source.vmId(), null);
        }
        addSpec(specs, definitions, parent, ComputerSpec.BIOS_MANUFACTURER, source.biosManufacturer(), null);
        addSpec(specs, definitions, parent, ComputerSpec.BIOS_VERSION, source.biosVersion(), null);
        addSpec(specs, definitions, parent, ComputerSpec.BIOS_RELEASE_DATE, source.biosReleaseDate(), null);
        BigDecimal cores = source.totalCpus() == null || source.corePerCpu() == null ? null
                : BigDecimal.valueOf((long) source.totalCpus() * source.corePerCpu());
        addSpec(specs, definitions, parent, ComputerSpec.CPU_CORES, cores, null);
        if (classification == CiClassification.GENERIC_SWITCH) {
            addSpec(specs, definitions, parent, GenericComputerSystemSpec.GENERIC_TYPE,
                    source.networkKind(), null);
        }
        return List.copyOf(specs);
    }

    private void addSpec(List<ActCiSpecUpsert> specs, CiDefinitionCache definitions,
                         ActCiUpsert parent, CiSpec field, Object value, String unit) {
        specMapper.addSpec(specs, definitions, parent, field, value, unit);
    }

    private static BigDecimal decimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static String memoryUnit(String unit) {
        return switch (unit == null ? "" : unit.trim()) {
            case "GB" -> "GBYTE";
            case "MB" -> "MBYTE";
            default -> null;
        };
    }

    private static String speedUnit(String unit) {
        return switch (unit == null ? "" : unit.trim()) {
            case "GHz" -> "GHZ";
            case "MHz" -> "MHZ";
            default -> null;
        };
    }
}
