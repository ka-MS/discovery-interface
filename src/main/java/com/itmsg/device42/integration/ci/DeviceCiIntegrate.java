package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.DeviceSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.CiSpec;
import com.itmsg.device42.enums.ci.ComputerSpec;
import com.itmsg.device42.enums.ci.ComputerSystemClusterSpec;
import com.itmsg.device42.enums.ci.GenericComputerSystemSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeviceCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(DeviceCiIntegrate.class);
    static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public DeviceCiIntegrate(
            Device42ConnectionFactory connectionFactory,
            ActCiWriter writer,
            CiSpecMapper specMapper
    ) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
        this.specMapper = specMapper;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 Device 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 Device 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("Device 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<DeviceSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 Device가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("Device CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("Device 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DeviceSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<DeviceSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long devicePk = rs.getLong("device_pk");
                try {
                    data.add(readDevice(rs));
                } catch (SQLException | ArithmeticException e) {
                    log.error("Device 원천 변환에 실패했습니다. devicePk={}", devicePk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("Device 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<DeviceSource> data, CiDefinitionCache definitions) {
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
                ClassificationDefinition definition = definitions.classification(classification);
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

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
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

    private static DeviceSource readDevice(ResultSet rs) throws SQLException {
        return new DeviceSource(
                rs.getLong("device_pk"), rs.getString("type"), rs.getString("physicalsubtype"),
                nullableBoolean(rs, "network_device"), nullableLong(rs, "cluster_pk"),
                rs.getString("network_kind"), nullableInteger(rs, "network_kind_count"),
                nullableInteger(rs, "cluster_count"), rs.getString("snmp_location"),
                rs.getString("name"), rs.getString("notes"),
                rs.getString("serial_no"), rs.getString("uuid"), rs.getString("last_discovered"),
                rs.getString("model"), rs.getString("manufacturer"), rs.getBigDecimal("ram"),
                rs.getString("ram_size_type"), nullableInteger(rs, "total_cpus"),
                nullableInteger(rs, "core_per_cpu"), rs.getBigDecimal("cpu_speed"),
                rs.getString("cpu_speed_unit"), rs.getString("cpu_type"),
                rs.getString("architecture"), rs.getString("primary_mac"), rs.getString("vm_id"),
                rs.getString("bios_manufacturer"), rs.getString("bios_version"),
                rs.getString("bios_release_date")
        );
    }

    private ActCiUpsert mapActCi(DeviceSource source, ClassificationDefinition definition,
                                 LocalDateTime applyDateTime) {
        LocalDateTime lastScan = SourceTimestamp.toLocalDateTime(source.lastDiscovered());
        return new ActCiUpsert(
                "D42:DEVICE:" + source.devicePk(), source.name(), definition.classStructureId(),
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

    private static Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.intValueExact();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.longValueExact();
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

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*) FROM view_device_v2 d WHERE
            """ + CiSourceFilter.DEVICE;

    private static final String SOURCE_QUERY = """
            WITH device AS (
                SELECT d.*
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.DEVICE + """
            ), cpu AS (
                SELECT p.device_fk,
                    COUNT(DISTINCT NULLIF(TRIM(pm.name), '')) AS model_count,
                    MIN(NULLIF(TRIM(pm.name), '')) AS cpu_model,
                    COUNT(DISTINCT NULLIF(TRIM(p.details->>'architecture'), '')) AS arch_count,
                    MIN(NULLIF(TRIM(p.details->>'architecture'), '')) AS architecture
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN device d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'CPU'
                GROUP BY p.device_fk
            ), primary_port AS (
                SELECT n.device_fk, COUNT(*) AS default_port_count,
                    MIN(NULLIF(TRIM(n.hwaddress), '')) AS primary_mac
                FROM view_netport_v1 n
                JOIN device d ON d.device_pk = n.device_fk
                WHERE n.is_default = true
                GROUP BY n.device_fk
            ), network_info AS (
                SELECT n.second_device_fk AS physical_pk,
                    CASE WHEN COUNT(DISTINCT n.device_fk) = 1 THEN MIN(n.device_fk) END AS cluster_pk,
                    COUNT(DISTINCT n.device_fk) AS cluster_count,
                    COUNT(DISTINCT NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind_count,
                    MIN(NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind,
                    MIN(NULLIF(TRIM(n.hwaddress), '')) AS network_mac
                FROM view_netport_v1 n
                JOIN device d ON d.device_pk = n.second_device_fk
                JOIN view_device_v2 c ON c.device_pk = n.device_fk
                WHERE d.type = 'physical' AND d.network_device = true
                  AND c.type = 'cluster' AND c.network_device = true
                GROUP BY n.second_device_fk
            )
            SELECT d.device_pk, d.type, d.physicalsubtype, d.network_device,
                ni.cluster_pk,
                CASE WHEN d.type = 'cluster'
                     THEN NULLIF(TRIM(d.details->>'fw_device_type'), '')
                     ELSE ni.network_kind END AS network_kind,
                CASE WHEN d.type = 'cluster'
                          AND NULLIF(TRIM(d.details->>'fw_device_type'), '') IS NOT NULL
                     THEN 1 ELSE ni.network_kind_count END AS network_kind_count,
                ni.cluster_count,
                NULLIF(TRIM(d.details->>'snmp_location'), '') AS snmp_location,
                'D42:DEVICE:' || CAST(d.device_pk AS varchar) AS source_id,
                d.name, d.notes, d.serial_no, d.uuid, d.last_discovered,
                h.name AS model, v.name AS manufacturer,
                d.ram, d.ram_size_type, d.total_cpus, d.core_per_cpu,
                CAST(d.total_cpus AS bigint) * d.core_per_cpu AS total_cores,
                d.cpu_speed, d.hz AS cpu_speed_unit,
                CASE WHEN cpu.model_count = 1 THEN cpu.cpu_model END AS cpu_type,
                CASE WHEN cpu.arch_count = 1 THEN cpu.architecture END AS architecture,
                CASE WHEN d.network_device = true THEN ni.network_mac
                     WHEN pp.default_port_count = 1 THEN pp.primary_mac END AS primary_mac,
                'ComputerSystem' AS system_type,
                CASE d.type WHEN 'virtual' THEN 'true' ELSE 'false' END AS is_virtual,
                CASE WHEN d.type = 'virtual' THEN d.vm_manager_int_id END AS vm_id,
                b.name AS bios_manufacturer, d.bios_version, d.bios_release_date,
                cpu.model_count, cpu.arch_count, pp.default_port_count
            FROM device d
            LEFT JOIN view_hardware_v2 h ON h.hardware_pk = d.hardware_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
            LEFT JOIN view_vendor_v1 b ON b.vendor_pk = d.bios_vendor_fk
            LEFT JOIN cpu ON cpu.device_fk = d.device_pk
            LEFT JOIN primary_port pp ON pp.device_fk = d.device_pk
            LEFT JOIN network_info ni ON ni.physical_pk = d.device_pk
            ORDER BY d.device_pk
            LIMIT %d OFFSET %d
            """;
}
