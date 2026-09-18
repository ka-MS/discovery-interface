package com.itmsg.device42.source.device42.selection;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** D42 장비 조건. 포함할 값은 호출자가 결정하고 SQL 표현은 원천이 소유한다. */
public final class DeviceSelection {
    private final String predicate;

    private DeviceSelection(String predicate) {
        this.predicate = predicate;
    }

    public static DeviceSelection all() {
        return new DeviceSelection("TRUE");
    }

    public static DeviceSelection discovered(List<String> types, Integer excludedVirtualSubtypeId,
                                              Boolean network, List<String> excludedPhysicalSubtypes) {
        String sql = "d.type IN (" + literals(types) + ")\n";
        if (excludedVirtualSubtypeId != null) {
            sql += "AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> " + excludedVirtualSubtypeId + ")\n";
        }
        if (network != null) {
            sql += network ? "AND d.network_device = true\n"
                    : "AND (d.network_device = false OR d.network_device IS NULL)\n";
        }
        if (!excludedPhysicalSubtypes.isEmpty()) {
            sql += "AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ("
                    + literals(excludedPhysicalSubtypes) + "))\n";
        }
        return new DeviceSelection(sql);
    }

    public static DeviceSelection compute(List<String> physicalSubtypes, List<String> virtualSubtypes) {
        return new DeviceSelection("""
                d.type IN ('physical', 'virtual')
                AND (d.network_device = false OR d.network_device IS NULL)
                AND (
                    (d.type = 'physical' AND d.physicalsubtype IN (%s))
                    OR (d.type = 'virtual' AND d.virtualsubtype IN (%s))
                )
                """.formatted(literals(physicalSubtypes), literals(virtualSubtypes)));
    }

    public DeviceSelection includingNetwork(String clusterKind) {
        return new DeviceSelection("(" + predicate + ")\nOR (d.type = 'physical' AND d.network_device = true)\n"
                + "OR (d.type = 'cluster' AND d.network_device = true\n"
                + "AND NULLIF(TRIM(d.details->>'fw_device_type'), '') = " + literal(clusterKind) + ")\n");
    }

    public DeviceSelection physicalSubtype(String subtype) {
        return new DeviceSelection("(" + predicate + ") AND d.physicalsubtype = " + literal(subtype) + "\n");
    }

    public String sql() {
        return predicate;
    }

    public static String literal(String value) {
        return "'" + Objects.requireNonNull(value).replace("'", "''") + "'";
    }

    public static String literals(List<String> values) {
        if (values.isEmpty()) throw new IllegalArgumentException("빈 IN 조건은 지원하지 않습니다.");
        return values.stream().map(DeviceSelection::literal).collect(Collectors.joining(", "));
    }
}
