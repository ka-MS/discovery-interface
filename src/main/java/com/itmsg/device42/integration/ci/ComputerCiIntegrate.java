package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.ComputerSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.dto.maximo.ci.SpecDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ComputerCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(ComputerCiIntegrate.class);
    static final String PHYSICAL = "SYS.COMPUTERSYSTEM";
    static final String VIRTUAL = "SYS.VIRTUALCOMPUTERSYSTEM";
    private static final String PREFIX = "COMPUTERSYSTEM_";
    private static final Set<String> NUMERIC_ATTRIBUTES =
            Set.of("MEMORYSIZE", "NUMCPUS", "CPUSPEED", "CPUCORESINSTALLED");
    private static final Set<String> TEXT_ATTRIBUTES = Set.of(
            "NAME", "SERIALNUMBER", "UUID", "MANUFACTURER", "MODEL", "CPUTYPE",
            "ARCHITECTURE", "PRIMARYMACADDRESS", "TYPE", "VIRTUAL", "VMID",
            "BIOSMANUFACTURER", "ROMVERSION", "BIOSRELEASEDATE"
    );
    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate jdbc;
    private final Environment environment;
    private final TransactionTemplate transaction;

    public ComputerCiIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate jdbc,
            Environment environment
    ) {
        this.connectionFactory = connectionFactory;
        this.jdbc = jdbc;
        this.environment = environment;
        this.transaction = new TransactionTemplate(
                new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource())));
    }

    @Override
    public void integrate() {
        CiLoadSettings settings = CiLoadSettings.from(environment);
        Map<String, ClassificationDefinition> definitions = loadDefinitions();
        long afterDevicePk = 0;
        long saved = 0;
        while (true) {
            List<ComputerSource> page = getData(afterDevicePk, settings.pageSize());
            if (page.isEmpty()) {
                break;
            }
            for (ComputerSource source : page) {
                if (source.devicePk() <= afterDevicePk) {
                    throw new IllegalStateException("Computer 조회 키가 중복되거나 정렬되지 않았습니다.");
                }
                saveComputer(source, definitions, settings);
                afterDevicePk = source.devicePk();
                saved++;
            }
        }
        log.info("Computer CI 적재가 완료되었습니다. computers={}", saved);
    }

    public List<ComputerSource> getData(long afterDevicePk, int limit) {
        if (afterDevicePk < 0 || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("유효하지 않은 Computer 조회 범위입니다.");
        }
        String query = SOURCE_QUERY.formatted(afterDevicePk, limit);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<ComputerSource> page = new ArrayList<>();
            while (rs.next()) {
                page.add(readComputer(rs));
            }
            return page;
        } catch (SQLException e) {
            throw new IllegalStateException("D42 Computer 조회에 실패했습니다. afterDevicePk=" + afterDevicePk, e);
        }
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

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.intValueExact();
    }

    Map<String, ClassificationDefinition> loadDefinitions() {
        Map<String, ClassificationDefinition> result = new LinkedHashMap<>();
        jdbc.query(CLASS_QUERY, rs -> {
            String name = rs.getString("CLASSIFICATIONID");
            String id = rs.getString("CLASSSTRUCTUREID");
            if (result.putIfAbsent(name, new ClassificationDefinition(name, id, Map.of())) != null) {
                throw new IllegalStateException("ACTCI 분류가 중복됩니다: " + name);
            }
        });
        if (!result.keySet().equals(Set.of(PHYSICAL, VIRTUAL))) {
            throw new IllegalStateException("물리·가상 Computer의 ACTCI 적용 분류가 필요합니다.");
        }
        Map<String, Map<String, SpecDefinition>> specs = new LinkedHashMap<>();
        result.keySet().forEach(name -> specs.put(name, new LinkedHashMap<>()));
        jdbc.query(SPEC_QUERY, rs -> {
            String attribute = rs.getString("ASSETATTRID");
            if (!selectedAttribute(attribute)) {
                return;
            }
            String name = rs.getString("CLASSIFICATIONID");
            String section = rs.getString("SECTION");
            String expected = NUMERIC_ATTRIBUTES.contains(attribute.substring(PREFIX.length())) ? "NUMERIC" : "ALN";
            String type = rs.getString("DATATYPE");
            if (!result.get(name).classStructureId().equals(rs.getString("CLASSSTRUCTUREID"))
                    || !expected.equals(type) || !"ACTCI".equals(rs.getString("OBJECTNAME"))
                    || rs.getInt("USEINSPEC") != 1 || section != null
                    || rs.getObject("SEQUENCE") == null || rs.getObject("MANDATORY") == null
                    || !rs.getString("CLASSSTRUCTUREID").equals(rs.getString("USE_CLASS"))
                    || !attribute.equals(rs.getString("USE_ATTRIBUTE"))
                    || rs.getString("USE_SECTION") != null
                    || (rs.getInt("MANDATORY") != 0 && rs.getInt("MANDATORY") != 1)
                    || rs.getInt("SEQUENCE") < Short.MIN_VALUE || rs.getInt("SEQUENCE") > Short.MAX_VALUE) {
                throw new IllegalStateException("사용할 수 없는 ACTCI 스펙 정의입니다: " + name + "/" + attribute);
            }
            SpecDefinition spec = new SpecDefinition(
                    rs.getLong("CLASSSPECID"), rs.getString("CLASSSTRUCTUREID"), attribute, type,
                    section, rs.getString("MEASUREUNITID"), rs.getInt("SEQUENCE"),
                    rs.getInt("MANDATORY") == 1, rs.getString("LINKEDTOATTRIBUTE"), rs.getString("LINKEDTOSECTION")
            );
            if (specs.get(name).putIfAbsent(attribute, spec) != null) {
                throw new IllegalStateException("ACTCI 스펙 템플릿이 중복됩니다: " + name + "/" + attribute);
            }
        });
        for (String name : result.keySet()) {
            for (String suffix : allAttributes()) {
                if (!specs.get(name).containsKey(PREFIX + suffix)) {
                    throw new IllegalStateException("ACTCI 스펙 등록이 필요합니다: " + name + "/" + PREFIX + suffix);
                }
            }
            result.put(name, new ClassificationDefinition(name, result.get(name).classStructureId(), specs.get(name)));
        }
        Set<String> units = Set.copyOf(jdbc.queryForList(
                "SELECT MEASUREUNITID FROM MAXIMO.MEASUREUNIT WHERE MEASUREUNITID IN ('GBYTE','MBYTE','GHZ','MHZ')",
                String.class));
        if (!units.containsAll(Set.of("GBYTE", "MBYTE", "GHZ", "MHZ"))) {
            throw new IllegalStateException("Computer 메모리·속도 단위 코드가 필요합니다.");
        }
        return Map.copyOf(result);
    }

    private static List<String> allAttributes() {
        List<String> attributes = new ArrayList<>(TEXT_ATTRIBUTES);
        attributes.addAll(NUMERIC_ATTRIBUTES);
        return attributes;
    }

    private static boolean selectedAttribute(String attribute) {
        return attribute != null && attribute.startsWith(PREFIX)
                && (TEXT_ATTRIBUTES.contains(attribute.substring(PREFIX.length()))
                || NUMERIC_ATTRIBUTES.contains(attribute.substring(PREFIX.length())));
    }

    void saveComputer(ComputerSource source, Map<String, ClassificationDefinition> definitions, CiLoadSettings settings) {
        String name = switch (source.type()) {
            case "physical" -> PHYSICAL;
            case "virtual" -> VIRTUAL;
            default -> throw new IllegalArgumentException("지원하지 않는 Computer 유형입니다: " + source.type());
        };
        ClassificationDefinition definition = Objects.requireNonNull(definitions.get(name), name);
        LocalDateTime changedAt = LocalDateTime.now(settings.zoneId());
        ActCiUpsert parent = mapActCi(source, definition, settings, changedAt);
        transaction.executeWithoutResult(status -> {
            long id = saveActCi(parent);
            for (ActCiSpecUpsert spec : mapSpecs(source, id, parent, definition)) {
                saveSpec(spec);
            }
        });
    }

    static ActCiUpsert mapActCi(ComputerSource source, ClassificationDefinition definition,
                               CiLoadSettings settings, LocalDateTime changedAt) {
        if (source.devicePk() <= 0) {
            throw new IllegalArgumentException("Computer 원천 PK가 필요합니다.");
        }
        String sourceId = "D42:DEVICE:" + source.devicePk();
        if (source.lastDiscovered() == null || source.lastDiscovered().isBlank()) {
            throw new IllegalArgumentException("LASTSCANDT 원천이 없습니다: " + sourceId);
        }
        String timestamp = source.lastDiscovered().trim().replace(' ', 'T')
                .replaceFirst("([+-]\\d{2})$", "$1:00");
        LocalDateTime lastScan = OffsetDateTime.parse(timestamp)
                .atZoneSameInstant(settings.zoneId()).toLocalDateTime();
        return new ActCiUpsert(
                sourceId, checkedText(source.name(), 192, "ACTCINAME"), definition.classStructureId(),
                checkedText(source.notes(), 1024, "DESCRIPTION"), lastScan,
                settings.changeBy(), changedAt, settings.langCode()
        );
    }

    static List<ActCiSpecUpsert> mapSpecs(ComputerSource source, long id,
                                         ActCiUpsert parent, ClassificationDefinition definition) {
        List<ActCiSpecUpsert> specs = new ArrayList<>();
        addSpec(specs, definition, parent, id, "NAME", source.name(), null);
        addSpec(specs, definition, parent, id, "SERIALNUMBER", source.serialNo(), null);
        addSpec(specs, definition, parent, id, "UUID", source.uuid(), null);
        addSpec(specs, definition, parent, id, "MODEL", source.model(), null);
        addSpec(specs, definition, parent, id, "MANUFACTURER", source.manufacturer(), null);
        addSpec(specs, definition, parent, id, "MEMORYSIZE", source.ram(),
                source.ram() == null ? null : memoryUnit(source.ramUnit()));
        addSpec(specs, definition, parent, id, "NUMCPUS", decimal(source.totalCpus()), null);
        addSpec(specs, definition, parent, id, "CPUSPEED", source.cpuSpeed(),
                source.cpuSpeed() == null ? null : speedUnit(source.cpuSpeedUnit()));
        addSpec(specs, definition, parent, id, "CPUTYPE", source.cpuType(), null);
        addSpec(specs, definition, parent, id, "ARCHITECTURE", source.architecture(), null);
        addSpec(specs, definition, parent, id, "PRIMARYMACADDRESS", source.primaryMac(), null);
        addSpec(specs, definition, parent, id, "TYPE", "ComputerSystem", null);
        addSpec(specs, definition, parent, id, "VIRTUAL", Boolean.toString("virtual".equals(source.type())), null);
        addSpec(specs, definition, parent, id, "VMID", "virtual".equals(source.type()) ? source.vmId() : null, null);
        addSpec(specs, definition, parent, id, "BIOSMANUFACTURER", source.biosManufacturer(), null);
        addSpec(specs, definition, parent, id, "ROMVERSION", source.biosVersion(), null);
        addSpec(specs, definition, parent, id, "BIOSRELEASEDATE", source.biosReleaseDate(), null);
        BigDecimal cores = source.totalCpus() == null || source.corePerCpu() == null ? null
                : BigDecimal.valueOf((long) source.totalCpus() * source.corePerCpu());
        addSpec(specs, definition, parent, id, "CPUCORESINSTALLED", cores, null);
        return List.copyOf(specs);
    }

    private static void addSpec(List<ActCiSpecUpsert> specs, ClassificationDefinition definition,
                                ActCiUpsert parent, long id, String suffix, Object value, String unit) {
        SpecDefinition template = Objects.requireNonNull(definition.specs().get(PREFIX + suffix), PREFIX + suffix);
        if (value == null || value instanceof String text && text.isBlank()) {
            if (template.mandatory()) {
                throw new IllegalArgumentException("필수 스펙 값이 없습니다: " + template.assetAttrId());
            }
            return;
        }
        String text = null;
        BigDecimal number = null;
        if ("ALN".equals(template.dataType()) && value instanceof String string) {
            text = checkedText(string, 254, template.assetAttrId());
        } else if ("NUMERIC".equals(template.dataType()) && value instanceof BigDecimal decimal) {
            number = decimal.stripTrailingZeros();
            if (number.precision() - number.scale() > 25 || number.scale() > 5) {
                throw new IllegalArgumentException("NUMVALUE(30,5) 범위를 초과했습니다: " + template.assetAttrId());
            }
        } else {
            throw new IllegalArgumentException("속성과 값의 자료형이 다릅니다: " + template.assetAttrId());
        }
        specs.add(new ActCiSpecUpsert(
                parent.actCiNum(), id, parent.classStructureId(), template.assetAttrId(),
                template.classSpecId(), template.section(), template.displaySequence(), template.mandatory(),
                unit == null ? template.measureUnitId() : unit,
                template.linkedToAttribute(), template.linkedToSection(), text, number,
                parent.changeBy(), parent.changeDate()
        ));
    }

    private static String checkedText(String text, int limit, String field) {
        if (text != null && text.length() > limit) {
            throw new IllegalArgumentException(field + " 길이를 초과했습니다. limit=" + limit);
        }
        return text;
    }

    private static BigDecimal decimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static String memoryUnit(String unit) {
        return switch (unit == null ? "" : unit.trim()) {
            case "GB" -> "GBYTE";
            case "MB" -> "MBYTE";
            default -> throw new IllegalArgumentException("미지원 메모리 단위입니다: " + unit);
        };
    }

    private static String speedUnit(String unit) {
        return switch (unit == null ? "" : unit.trim()) {
            case "GHz" -> "GHZ";
            case "MHz" -> "MHZ";
            default -> throw new IllegalArgumentException("미지원 CPU 속도 단위입니다: " + unit);
        };
    }

    private long saveActCi(ActCiUpsert ci) {
        List<ExistingCi> existing = jdbc.query(
                "SELECT ACTCIID,CLASSSTRUCTUREID FROM MAXIMO.ACTCI WHERE ACTCINUM=?",
                (rs, row) -> new ExistingCi(rs.getLong(1), rs.getString(2)), ci.actCiNum());
        if (existing.size() > 1) {
            throw new IllegalStateException("ACTCINUM이 중복됩니다: " + ci.actCiNum());
        }
        if (!existing.isEmpty()) {
            ExistingCi old = existing.getFirst();
            if (!ci.classStructureId().equals(old.classStructureId())) {
                throw new IllegalStateException("분류 변경 규칙이 필요합니다: " + ci.actCiNum());
            }
            jdbc.update("""
                    UPDATE MAXIMO.ACTCI SET ACTCINAME=?,DESCRIPTION=?,LASTSCANDT=?,
                        CHANGEBY=?,CHANGEDATE=?,LANGCODE=?
                    WHERE ACTCIID=?
                    """, ci.actCiName(), ci.description(), ci.lastScanDate(), ci.changeBy(),
                    ci.changeDate(), ci.langCode(), old.id());
            return old.id();
        }
        long id = Objects.requireNonNull(jdbc.queryForObject("VALUES NEXT VALUE FOR MAXIMO.ACTCISEQ", Long.class));
        jdbc.update("""
                INSERT INTO MAXIMO.ACTCI
                    (ACTCIID,ACTCINUM,ACTCINAME,CLASSSTRUCTUREID,DESCRIPTION,
                     LASTSCANDT,CHANGEBY,CHANGEDATE,LANGCODE,HASLD)
                VALUES (?,?,?,?,?,?,?,?,?,0)
                """, id, ci.actCiNum(), ci.actCiName(), ci.classStructureId(), ci.description(),
                ci.lastScanDate(), ci.changeBy(), ci.changeDate(), ci.langCode());
        return id;
    }

    private void saveSpec(ActCiSpecUpsert spec) {
        List<Long> ids = jdbc.queryForList("""
                SELECT ACTCISPECID FROM MAXIMO.ACTCISPEC
                WHERE ACTCINUM=? AND ASSETATTRID=?
                  AND (SECTION=? OR (SECTION IS NULL AND ?=1))
                """, Long.class, spec.actCiNum(), spec.assetAttrId(), spec.section(), spec.section() == null ? 1 : 0);
        if (ids.size() > 1) {
            throw new IllegalStateException("ACTCISPEC 키가 중복됩니다: " + spec.actCiNum() + "/" + spec.assetAttrId());
        }
        if (ids.isEmpty()) {
            long id = Objects.requireNonNull(jdbc.queryForObject("VALUES NEXT VALUE FOR MAXIMO.ACTCISPECSEQ", Long.class));
            jdbc.update("""
                    INSERT INTO MAXIMO.ACTCISPEC
                        (ACTCISPECID,ACTCINUM,ASSETATTRID,CLASSSTRUCTUREID,CLASSSPECID,SECTION,
                         REFOBJECTID,REFOBJECTNAME,DISPLAYSEQUENCE,MANDATORY,MEASUREUNITID,
                         LINKEDTOATTRIBUTE,LINKEDTOSECTION,ALNVALUE,NUMVALUE,TABLEVALUE,CHANGEBY,CHANGEDATE)
                    VALUES (?,?,?,?,?,?,?,'ACTCI',?,?,?,?,?,?,?,NULL,?,?)
                    """, id, spec.actCiNum(), spec.assetAttrId(), spec.classStructureId(), spec.classSpecId(),
                    spec.section(), spec.refObjectId(), spec.displaySequence(), spec.mandatory() ? 1 : 0,
                    spec.measureUnitId(), spec.linkedToAttribute(), spec.linkedToSection(),
                    spec.alnValue(), spec.numValue(), spec.changeBy(), spec.changeDate());
        } else {
            jdbc.update("""
                    UPDATE MAXIMO.ACTCISPEC SET CLASSSTRUCTUREID=?,CLASSSPECID=?,
                        REFOBJECTID=?,REFOBJECTNAME='ACTCI',DISPLAYSEQUENCE=?,MANDATORY=?,
                        MEASUREUNITID=?,LINKEDTOATTRIBUTE=?,LINKEDTOSECTION=?,
                        ALNVALUE=?,NUMVALUE=?,TABLEVALUE=NULL,CHANGEBY=?,CHANGEDATE=?
                    WHERE ACTCISPECID=?
                    """, spec.classStructureId(), spec.classSpecId(), spec.refObjectId(),
                    spec.displaySequence(), spec.mandatory() ? 1 : 0, spec.measureUnitId(),
                    spec.linkedToAttribute(), spec.linkedToSection(), spec.alnValue(),
                    spec.numValue(), spec.changeBy(), spec.changeDate(), ids.getFirst());
        }
    }

    private record ExistingCi(long id, String classStructureId) {
    }

    static final String CLASS_QUERY = """
            SELECT s.CLASSIFICATIONID,s.CLASSSTRUCTUREID
            FROM MAXIMO.CLASSSTRUCTURE s
            JOIN MAXIMO.CLASSUSEWITH u ON u.CLASSSTRUCTUREID=s.CLASSSTRUCTUREID
            WHERE s.CLASSIFICATIONID IN ('SYS.COMPUTERSYSTEM','SYS.VIRTUALCOMPUTERSYSTEM')
              AND u.OBJECTNAME='ACTCI'
            """;

    static final String SPEC_QUERY = """
            SELECT s.CLASSIFICATIONID,c.CLASSSTRUCTUREID,c.CLASSSPECID,c.ASSETATTRID,
                a.DATATYPE,c.SECTION,c.MEASUREUNITID,c.LINKEDTOATTRIBUTE,c.LINKEDTOSECTION,
                u.OBJECTNAME,u.SEQUENCE,u.MANDATORY,u.USEINSPEC,
                u.CLASSSTRUCTUREID AS USE_CLASS,u.ASSETATTRID AS USE_ATTRIBUTE,u.SECTION AS USE_SECTION
            FROM MAXIMO.CLASSSTRUCTURE s
            JOIN MAXIMO.CLASSSPEC c ON c.CLASSSTRUCTUREID=s.CLASSSTRUCTUREID
            LEFT JOIN MAXIMO.ASSETATTRIBUTE a
              ON a.ASSETATTRIBUTEID=c.ASSETATTRIBUTEID AND a.ASSETATTRID=c.ASSETATTRID
            LEFT JOIN MAXIMO.CLASSSPECUSEWITH u ON u.CLASSSPECID=c.CLASSSPECID AND u.OBJECTNAME='ACTCI'
            WHERE s.CLASSIFICATIONID IN ('SYS.COMPUTERSYSTEM','SYS.VIRTUALCOMPUTERSYSTEM')
            """;

    static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.*
                FROM view_device_v2 d
                WHERE d.type IN ('physical', 'virtual')
                  AND (d.network_device = false OR d.network_device IS NULL)
                  AND (
                      (d.type = 'physical' AND d.physicalsubtype IN
                          ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
                      OR
                      (d.type = 'virtual' AND d.virtualsubtype IN
                          ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
                  )
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
                CASE d.type WHEN 'physical' THEN 'SYS.COMPUTERSYSTEM'
                            WHEN 'virtual' THEN 'SYS.VIRTUALCOMPUTERSYSTEM' END AS classification_id,
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
            WHERE d.device_pk > %d
            ORDER BY d.device_pk
            LIMIT %d
            """;
}
