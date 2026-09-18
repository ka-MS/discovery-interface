package com.itmsg.device42.source.device42.selection;

import java.util.List;

/** 본체와 관계에 같은 제외 목록을 전달한다. 목록의 정책은 원천 조회가 결정하지 않는다. */
public record FilesystemFilter(List<String> excludedTypes) {
    public FilesystemFilter {
        excludedTypes = List.copyOf(excludedTypes);
    }

    public String sql(boolean caseInsensitive) {
        if (excludedTypes.isEmpty()) return "TRUE";
        String values = DeviceSelection.literals(excludedTypes);
        return caseInsensitive ? "LOWER(COALESCE(m.fstype_name, '')) NOT IN (" + values + ")"
                : "(m.fstype_name IS NULL OR m.fstype_name NOT IN (" + values + "))";
    }
}
