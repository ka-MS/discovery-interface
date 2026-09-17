package com.itmsg.device42.integration.ci;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** D42 타임스탬프 문자열을 JVM 기본 시간대의 LocalDateTime으로 바꾼다. */
public final class SourceTimestamp {
    private SourceTimestamp() {
    }

    /** @return 값이 없으면 null. 형식이 틀리면 DateTimeParseException을 던진다. */
    public static LocalDateTime toLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String timestamp = value.trim().replace(' ', 'T').replaceFirst("([+-]\\d{2})$", "$1:00");
        return OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
