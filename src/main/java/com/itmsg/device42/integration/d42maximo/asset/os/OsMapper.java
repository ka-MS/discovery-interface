package com.itmsg.device42.integration.d42maximo.asset.os;

import com.itmsg.device42.maximo.asset.DpaOsUpsert;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OsMapper {

    private static final String UNKNOWN = "UNKNOWN";

    public List<DpaOsUpsert> mapData(List<OperatingSystemSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaOsUpsert> mappedData = new ArrayList<>(data.size());

        for (OperatingSystemSource source : data) {
            mappedData.add(new DpaOsUpsert(
                    source.deviceosPk(),
                    source.deviceFk(),
                    trimToNull(source.osVersionNo()),
                    defaultUnknown(source.vendorName()),
                    defaultUnknown(source.osName()),
                    trimToNull(source.osVersion()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static String defaultUnknown(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? UNKNOWN : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
