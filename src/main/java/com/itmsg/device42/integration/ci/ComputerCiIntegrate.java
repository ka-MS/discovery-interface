package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.ComputerSpec;
import com.itmsg.device42.dto.device42.ci.ComputerSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.ComputerCiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.dto.maximo.ci.SpecDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate maximoJdbcTemplate;

    public ComputerCiIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
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
            List<ComputerCiUpsert> mappedData = mapData(data, definitions);

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

    List<ComputerCiUpsert> mapData(List<ComputerSource> data,
                                  CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<ComputerCiUpsert> mappedData = new ArrayList<>(data.size());

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
                mappedData.add(new ComputerCiUpsert(actCi, specs));
            } catch (RuntimeException e) {
                log.error("Computer 매핑에 실패했습니다. devicePk={}", source.devicePk(), e);
            }
        }
        return mappedData;
    }

    /** @return ACTCI 적재에 성공한 건수. 실패·건너뛴 건은 각각 로그로 남는다. */
    public int putData(List<ComputerCiUpsert> data) {
        int loaded = 0;

        for (ComputerCiUpsert computer : data) {
            ActCiUpsert actCi = computer.actCi();
            Long actCiId;
            try {
                actCiId = putActCi(actCi);
            } catch (DataAccessException e) {
                log.error("ACTCI 적재에 실패했습니다. actCiNum={}", actCi.actCiNum(), e);
                continue;
            }
            if (actCiId == null) {
                continue;
            }
            loaded++;

            for (ActCiSpecUpsert spec : computer.specs()) {
                try {
                    putActCiSpec(spec, actCiId);
                } catch (DataAccessException e) {
                    log.error("ACTCISPEC 적재에 실패했습니다. actCiNum={}, attribute={}",
                            spec.actCiNum(), spec.assetAttrId(), e);
                }
            }
        }
        return loaded;
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
        if (value == null || value instanceof String text && text.isBlank()) {
            return;
        }
        SpecDefinition template = definitions.spec(parent.classStructureId(), field.attributeId(), null);
        if (template == null && field.additionalDisplaySequence() != null) {
            template = definitions.additionalSpec(parent.classStructureId(), field.attributeId(), null,
                    field.additionalDisplaySequence(), field.additionalMandatory());
        }
        if (template == null) {
            log.warn("스펙 정의가 없어 건너뜁니다. actCiNum={}, attribute={}", parent.actCiNum(), field.attributeId());
            return;
        }
        if ((field == ComputerSpec.MEMORY_SIZE || field == ComputerSpec.CPU_SPEED) && unit == null) {
            log.warn("단위 코드가 없어 스펙을 건너뜁니다. actCiNum={}, attribute={}", parent.actCiNum(), template.assetAttrId());
            return;
        }
        String text = null;
        BigDecimal number = null;
        if ("ALN".equals(template.dataType()) && value instanceof String string) {
            text = string;
        } else if ("NUMERIC".equals(template.dataType()) && value instanceof BigDecimal decimal) {
            number = decimal;
        } else {
            log.warn("자료형이 맞지 않아 스펙을 건너뜁니다. actCiNum={}, attribute={}",
                    parent.actCiNum(), template.assetAttrId());
            return;
        }
        specs.add(new ActCiSpecUpsert(
                parent.actCiNum(), parent.classStructureId(), template.assetAttrId(),
                template.classSpecId(), template.section(), template.displaySequence(), template.mandatory(),
                unit == null ? template.measureUnitId() : unit,
                template.linkedToAttribute(), template.linkedToSection(), text, number,
                parent.changeBy(), parent.changeDate()));
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

    private Long putActCi(ActCiUpsert ci) {
        List<ExistingCi> existing = maximoJdbcTemplate.query(
                FIND_ACTCI_QUERY,
                (rs, row) -> new ExistingCi(rs.getLong(1), rs.getString(2)), ci.actCiNum());
        if (!existing.isEmpty()) {
            ExistingCi old = existing.getFirst();
            if (!ci.classStructureId().equals(old.classStructureId())) {
                log.warn("분류 변경 규칙이 없어 건너뜁니다. actCiNum={}", ci.actCiNum());
                return null;
            }
            maximoJdbcTemplate.update(UPDATE_ACTCI_QUERY, ci.actCiName(), ci.description(), ci.lastScanDate(), ci.changeBy(),
                    ci.changeDate(), ci.langCode(), old.id());
            return old.id();
        }
        long id = maximoJdbcTemplate.queryForObject(NEXT_ACTCI_ID_QUERY, Long.class);
        maximoJdbcTemplate.update(INSERT_ACTCI_QUERY, id, ci.actCiNum(), ci.actCiName(), ci.classStructureId(), ci.description(),
                ci.lastScanDate(), ci.changeBy(), ci.changeDate(), ci.langCode());
        return id;
    }

    private void putActCiSpec(ActCiSpecUpsert spec, long actCiId) {
        maximoJdbcTemplate.update(MERGE_ACTCISPEC_QUERY,
                spec.actCiNum(), spec.assetAttrId(), spec.section(), spec.classStructureId(),
                spec.classSpecId(), actCiId, spec.displaySequence(), spec.mandatory() ? 1 : 0,
                spec.measureUnitId(), spec.linkedToAttribute(), spec.linkedToSection(),
                spec.alnValue(), spec.numValue(), spec.changeBy(), spec.changeDate());
    }

    private record ExistingCi(long id, String classStructureId) {
    }

    private static final String FIND_ACTCI_QUERY = "SELECT ACTCIID,CLASSSTRUCTUREID FROM MAXIMO.ACTCI WHERE ACTCINUM=?";

    private static final String UPDATE_ACTCI_QUERY = """
            UPDATE MAXIMO.ACTCI SET ACTCINAME=?,DESCRIPTION=?,LASTSCANDT=?,
                CHANGEBY=?,CHANGEDATE=?,LANGCODE=?
            WHERE ACTCIID=?
            """;

    private static final String INSERT_ACTCI_QUERY = """
            INSERT INTO MAXIMO.ACTCI
                (ACTCIID,ACTCINUM,ACTCINAME,CLASSSTRUCTUREID,DESCRIPTION,
                 LASTSCANDT,CHANGEBY,CHANGEDATE,LANGCODE,HASLD)
            VALUES (?,?,?,?,?,?,?,?,?,0)
            """;

    private static final String NEXT_ACTCI_ID_QUERY = "VALUES NEXT VALUE FOR MAXIMO.ACTCISEQ";

    /**
     * 파라미터 마커에 CAST가 필요하다. USING 절의 마커는 DB2가 타입을 추론하지 못해
     * SQLCODE=-418로 거부한다. ACTCISPECID는 NOT MATCHED 분기에서만 채번한다.
     * USING 절에서는 NEXT VALUE FOR가 금지된다(SQLCODE=-348).
     */
    private static final String MERGE_ACTCISPEC_QUERY = """
            MERGE INTO MAXIMO.ACTCISPEC AS target
            USING (VALUES (
                CAST(? AS VARCHAR(150)), CAST(? AS VARCHAR(300)), CAST(? AS VARCHAR(10)),
                CAST(? AS VARCHAR(25)), CAST(? AS BIGINT), CAST(? AS BIGINT),
                CAST(? AS INTEGER), CAST(? AS INTEGER), CAST(? AS VARCHAR(16)),
                CAST(? AS VARCHAR(300)), CAST(? AS VARCHAR(10)), CAST(? AS VARCHAR(254)),
                CAST(? AS DECIMAL(30,10)), CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
            )) AS source (
                ACTCINUM, ASSETATTRID, SECTION, CLASSSTRUCTUREID, CLASSSPECID,
                REFOBJECTID, DISPLAYSEQUENCE, MANDATORY, MEASUREUNITID,
                LINKEDTOATTRIBUTE, LINKEDTOSECTION, ALNVALUE, NUMVALUE, CHANGEBY, CHANGEDATE
            )
            ON target.ACTCINUM = source.ACTCINUM
                AND target.ASSETATTRID = source.ASSETATTRID
                AND (target.SECTION = source.SECTION
                     OR (target.SECTION IS NULL AND source.SECTION IS NULL))
            WHEN MATCHED THEN
                UPDATE SET
                    CLASSSTRUCTUREID = source.CLASSSTRUCTUREID,
                    CLASSSPECID = source.CLASSSPECID,
                    REFOBJECTID = source.REFOBJECTID,
                    REFOBJECTNAME = 'ACTCI',
                    DISPLAYSEQUENCE = source.DISPLAYSEQUENCE,
                    MANDATORY = source.MANDATORY,
                    MEASUREUNITID = source.MEASUREUNITID,
                    LINKEDTOATTRIBUTE = source.LINKEDTOATTRIBUTE,
                    LINKEDTOSECTION = source.LINKEDTOSECTION,
                    ALNVALUE = source.ALNVALUE,
                    NUMVALUE = source.NUMVALUE,
                    TABLEVALUE = NULL,
                    CHANGEBY = source.CHANGEBY,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ACTCISPECID, ACTCINUM, ASSETATTRID, CLASSSTRUCTUREID, CLASSSPECID,
                    SECTION, REFOBJECTID, REFOBJECTNAME, DISPLAYSEQUENCE, MANDATORY,
                    MEASUREUNITID, LINKEDTOATTRIBUTE, LINKEDTOSECTION, ALNVALUE,
                    NUMVALUE, TABLEVALUE, CHANGEBY, CHANGEDATE
                ) VALUES (
                    NEXT VALUE FOR MAXIMO.ACTCISPECSEQ,
                    source.ACTCINUM, source.ASSETATTRID, source.CLASSSTRUCTUREID,
                    source.CLASSSPECID, source.SECTION, source.REFOBJECTID, 'ACTCI',
                    source.DISPLAYSEQUENCE, source.MANDATORY, source.MEASUREUNITID,
                    source.LINKEDTOATTRIBUTE, source.LINKEDTOSECTION, source.ALNVALUE,
                    source.NUMVALUE, NULL, source.CHANGEBY, source.CHANGEDATE
                )
            """;

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
