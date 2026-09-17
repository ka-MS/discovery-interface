package com.itmsg.device42.integration.d42maximo.ci.device;

import com.itmsg.device42.device42.DoqlClient;
import com.itmsg.device42.integration.d42maximo.ci.selection.CiSourceFilter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeviceCiQuery {

    private static final Logger log = LoggerFactory.getLogger(DeviceCiQuery.class);

    private final DoqlClient doql;

    public DeviceCiQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.query(TOTAL_COUNT_QUERY, rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Device 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DeviceSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try {
            return doql.query(query, rs -> {
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
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Device 조회에 실패했습니다. offset=" + offset, e);
        }
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
