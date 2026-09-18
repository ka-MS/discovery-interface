package com.itmsg.device42.pipeline.d42maximo.asset.cpu;

import com.itmsg.device42.source.device42.asset.cpu.ProcessorSource;

import com.itmsg.device42.target.maximo.asset.DpaCpuUpsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CpuMapper {

    private static final String UNKNOWN = "UNKNOWN";

    private static final BigDecimal ZERO_SPEED = new BigDecimal("0.00");

    public List<DpaCpuUpsert> mapData(List<ProcessorSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaCpuUpsert> mappedData = new ArrayList<>(data.size());

        for (ProcessorSource source : data) {
            mappedData.add(new DpaCpuUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    trimToNull(source.slot()),
                    ZERO_SPEED,
                    firstNonBlank(source.description(), source.modelName()),
                    0,
                    defaultUnknown(source.modelName()),
                    defaultUnknown(source.vendorName()),
                    roundSpeed(source.speed()),
                    source.cores(),
                    trimToNull(source.speedUnit()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static BigDecimal roundSpeed(BigDecimal value) {
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
