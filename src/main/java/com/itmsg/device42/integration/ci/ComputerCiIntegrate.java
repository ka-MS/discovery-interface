package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.ComputerSpec;
import com.itmsg.device42.dto.device42.ci.ComputerSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class ComputerCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(ComputerCiIntegrate.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public ComputerCiIntegrate(
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
            log.info("배치할 Computer 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 Computer 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("Computer 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<ComputerSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 Computer가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("Computer CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("Computer 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ComputerSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<ComputerSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long devicePk = rs.getLong("device_pk");
                try {
                    data.add(readComputer(rs));
                } catch (SQLException | ArithmeticException e) {
                    log.error("Computer 원천 변환에 실패했습니다. devicePk={}", devicePk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("Computer 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<ComputerSource> data,
                                  CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (ComputerSource source : data) {
            try {
                CiClassification classification = "virtual".equals(source.type())
                        ? CiClassification.VIRTUAL_COMPUTER : CiClassification.COMPUTER;
                ClassificationDefinition definition = definitions.classification(classification);
                if (definition == null) {
                    log.warn("Computer 분류가 없어 건너뜁니다. devicePk={}, classification={}",
                            source.devicePk(), classification);
                    continue;
                }

                ActCiUpsert actCi = mapActCi(source, definition, applyDateTime);
                List<ActCiSpecUpsert> specs = mapActCiSpecs(source, actCi, definitions, classification);
                mappedData.add(new CiUpsert(actCi, specs));
            } catch (RuntimeException e) {
                log.error("Computer 매핑에 실패했습니다. devicePk={}", source.devicePk(), e);
            }
        }
        return mappedData;
    }

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
    }

    private static ComputerSource readComputer(ResultSet rs) throws SQLException {
        return new ComputerSource(
                rs.getLong("device_pk"), rs.getString("type"), rs.getString("name"),
                rs.getString("notes"), rs.getString("serial_no"), rs.getString("uuid"),
                rs.getString("last_discovered"), rs.getString("model"), rs.getString("manufacturer"),
                rs.getBigDecimal("ram"), rs.getString("ram_size_type"),
                nullableInteger(rs, "total_cpus"), nullableInteger(rs, "core_per_cpu"),
                rs.getBigDecimal("cpu_speed"), rs.getString("cpu_speed_unit"),
                rs.getString("cpu_type"), rs.getString("architecture"), rs.getString("primary_mac"),
                rs.getString("vm_id"), rs.getString("bios_manufacturer"),
                rs.getString("bios_version"), rs.getString("bios_release_date")
        );
    }

    private ActCiUpsert mapActCi(ComputerSource source, ClassificationDefinition definition,
                                 LocalDateTime applyDateTime) {
        LocalDateTime lastScan = null;
        if (source.lastDiscovered() != null && !source.lastDiscovered().isBlank()) {
            String timestamp = source.lastDiscovered().trim().replace(' ', 'T')
                    .replaceFirst("([+-]\\d{2})$", "$1:00");
            lastScan = OffsetDateTime.parse(timestamp)
                    .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
        return new ActCiUpsert(
                "D42:DEVICE:" + source.devicePk(), source.name(), definition.classStructureId(),
                source.notes(), lastScan, CHANGE_BY, applyDateTime, LANG_CODE);
    }

    private List<ActCiSpecUpsert> mapActCiSpecs(ComputerSource source,
                                              ActCiUpsert parent, CiDefinitionCache definitions,
                                              CiClassification classification) {
        List<ActCiSpecUpsert> specs = new ArrayList<>();
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
        addSpec(specs, definitions, parent, ComputerSpec.VIRTUAL, Boolean.toString("virtual".equals(source.type())), null);
        if (ComputerSpec.VM_ID.appliesTo(classification)) {
            addSpec(specs, definitions, parent, ComputerSpec.VM_ID, source.vmId(), null);
        }
        addSpec(specs, definitions, parent, ComputerSpec.BIOS_MANUFACTURER, source.biosManufacturer(), null);
        addSpec(specs, definitions, parent, ComputerSpec.BIOS_VERSION, source.biosVersion(), null);
        addSpec(specs, definitions, parent, ComputerSpec.BIOS_RELEASE_DATE, source.biosReleaseDate(), null);
        BigDecimal cores = source.totalCpus() == null || source.corePerCpu() == null ? null
                : BigDecimal.valueOf((long) source.totalCpus() * source.corePerCpu());
        addSpec(specs, definitions, parent, ComputerSpec.CPU_CORES, cores, null);
        return List.copyOf(specs);
    }

    private void addSpec(List<ActCiSpecUpsert> specs, CiDefinitionCache definitions,
                         ActCiUpsert parent, ComputerSpec field, Object value, String unit) {
        specMapper.addSpec(specs, definitions, parent, field, value, unit);
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.intValueExact();
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

    private static final String COMPUTER_FILTER = """
            d.type IN ('physical', 'virtual')
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                (d.type = 'physical' AND d.physicalsubtype IN
                    ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
                OR
                (d.type = 'virtual' AND d.virtualsubtype IN
                    ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
            )
            """;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*) FROM view_device_v2 d WHERE
            """ + COMPUTER_FILTER;

    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.*
                FROM view_device_v2 d
                WHERE
            """ + COMPUTER_FILTER + """
            ), cpu AS (
                SELECT p.device_fk,
                    COUNT(DISTINCT NULLIF(TRIM(pm.name), '')) AS model_count,
                    MIN(NULLIF(TRIM(pm.name), '')) AS cpu_model,
                    COUNT(DISTINCT NULLIF(TRIM(p.details->>'architecture'), '')) AS arch_count,
                    MIN(NULLIF(TRIM(p.details->>'architecture'), '')) AS architecture
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN computer d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'CPU'
                GROUP BY p.device_fk
            ), primary_port AS (
                SELECT n.device_fk, COUNT(*) AS default_port_count,
                    MIN(NULLIF(TRIM(n.hwaddress), '')) AS primary_mac
                FROM view_netport_v1 n
                JOIN computer d ON d.device_pk = n.device_fk
                WHERE n.is_default = true
                GROUP BY n.device_fk
            )
            SELECT d.device_pk, d.type,
                'D42:DEVICE:' || CAST(d.device_pk AS varchar) AS source_id,
                d.name, d.notes, d.serial_no, d.uuid, d.last_discovered,
                h.name AS model, v.name AS manufacturer,
                d.ram, d.ram_size_type, d.total_cpus, d.core_per_cpu,
                CAST(d.total_cpus AS bigint) * d.core_per_cpu AS total_cores,
                d.cpu_speed, d.hz AS cpu_speed_unit,
                CASE WHEN cpu.model_count = 1 THEN cpu.cpu_model END AS cpu_type,
                CASE WHEN cpu.arch_count = 1 THEN cpu.architecture END AS architecture,
                CASE WHEN pp.default_port_count = 1 THEN pp.primary_mac END AS primary_mac,
                'ComputerSystem' AS system_type,
                CASE d.type WHEN 'virtual' THEN 'true' ELSE 'false' END AS is_virtual,
                CASE WHEN d.type = 'virtual' THEN d.vm_manager_int_id END AS vm_id,
                b.name AS bios_manufacturer, d.bios_version, d.bios_release_date,
                cpu.model_count, cpu.arch_count, pp.default_port_count
            FROM computer d
            LEFT JOIN view_hardware_v2 h ON h.hardware_pk = d.hardware_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
            LEFT JOIN view_vendor_v1 b ON b.vendor_pk = d.bios_vendor_fk
            LEFT JOIN cpu ON cpu.device_fk = d.device_pk
            LEFT JOIN primary_port pp ON pp.device_fk = d.device_pk
            ORDER BY d.device_pk
            LIMIT %d OFFSET %d
            """;
}
