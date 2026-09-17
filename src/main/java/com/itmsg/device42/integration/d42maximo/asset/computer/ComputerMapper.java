package com.itmsg.device42.integration.d42maximo.asset.computer;

import com.itmsg.device42.dto.device42.asset.ComputerHardwareSource;
import com.itmsg.device42.dto.maximo.asset.DpaComputerUpsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ComputerMapper {

    private static final Logger log = LoggerFactory.getLogger(ComputerMapper.class);

    private static final DateTimeFormatter US_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private static final DateTimeFormatter LEGACY_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public List<DpaComputerUpsert> mapData(List<ComputerHardwareSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaComputerUpsert> mappedData = new ArrayList<>(data.size());

        for (ComputerHardwareSource source : data) {
            mappedData.add(new DpaComputerUpsert(
                    (long) source.devicePk(),
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    source.biosName(),
                    source.biosVersion(),
                    parseBiosDate(source.biosReleaseDate()),
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    roundRamSize(source.ram()),
                    source.ramSizeType(),
                    0,
                    applyDateTime,
                    applyDateTime,
                    null,
                    null,
                    null,
                    null,
                    source.totalCpus(),
                    null,
                    null,
                    null,
                    calculateTotalCores(source.totalCpus(), source.corePerCpu()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return mappedData;
    }

    /** RAM 용량을 소수점 둘째 자리까지 반올림한다. */
    private static BigDecimal roundRamSize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** CPU 개수와 CPU당 코어 수를 곱해 전체 코어 수를 계산한다. */
    private static Integer calculateTotalCores(Integer totalCpus, Integer corePerCpu) {
        if (totalCpus == null || corePerCpu == null) {
            return null;
        }
        return Math.multiplyExact(totalCpus, corePerCpu);
    }

    /**
     * Device42에서 관측되는 BIOS 날짜 형식을 LocalDateTime으로 변환한다.
     * 값이 없거나 지원하지 않는 형식이면 null을 반환한다.
     */
    static LocalDateTime parseBiosDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(normalized, US_DATE_FORMATTER).atStartOfDay();
            } catch (DateTimeParseException ignoredUsFormat) {
                try {
                    return LocalDateTime.parse(normalized, LEGACY_DATE_TIME_FORMATTER);
                } catch (DateTimeParseException invalidFormat) {
                    log.warn("지원하지 않는 BIOS 날짜 형식입니다. value={}", value);
                    return null;
                }
            }
        }
    }
}
