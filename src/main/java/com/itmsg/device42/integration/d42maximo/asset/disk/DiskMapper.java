package com.itmsg.device42.integration.d42maximo.asset.disk;

import com.itmsg.device42.maximo.asset.DpaDiskUpsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiskMapper {

    private static final int FALSE = 0;

    private static final String UNKNOWN = "UNKNOWN";

    public List<DpaDiskUpsert> mapData(List<DiskSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaDiskUpsert> mappedData = new ArrayList<>(data.size());

        for (DiskSource source : data) {
            mappedData.add(new DpaDiskUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    firstNonBlank(source.description(), source.modelName()),
                    trimToNull(source.diskInterface()),
                    FALSE,
                    FALSE,
                    trimToNull(source.modelName()),
                    defaultUnknown(source.vendorName()),
                    FALSE,
                    trimToNull(source.serialNumber()),
                    trimToNull(source.sizeUnit()),
                    roundTotalSpace(source.totalSpace()),
                    FALSE,
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static BigDecimal roundTotalSpace(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    static String firstNonBlank(String primary, String fallback) {
        String normalizedPrimary = trimToNull(primary);
        return normalizedPrimary == null ? trimToNull(fallback) : normalizedPrimary;
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
