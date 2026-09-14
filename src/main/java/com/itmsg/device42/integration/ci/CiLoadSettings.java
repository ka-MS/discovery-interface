package com.itmsg.device42.integration.ci;

import org.springframework.core.env.Environment;

import java.time.ZoneId;

record CiLoadSettings(String changeBy, String langCode, ZoneId zoneId, int pageSize) {
    CiLoadSettings {
        if (changeBy == null || changeBy.isBlank() || changeBy.length() > 100) {
            throw new IllegalArgumentException("ci.change-by는 1~100자의 Maximo 실행 계정이어야 합니다.");
        }
        if (langCode == null || langCode.isBlank() || langCode.length() > 4) {
            throw new IllegalArgumentException("ci.lang-code는 1~4자의 Maximo 언어 코드여야 합니다.");
        }
        if (zoneId == null || pageSize < 1 || pageSize > 1000) {
            throw new IllegalArgumentException("ci.zone-id와 1~1000의 ci.page-size가 필요합니다.");
        }
    }

    static CiLoadSettings from(Environment environment) {
        return new CiLoadSettings(
                environment.getRequiredProperty("ci.change-by"),
                environment.getRequiredProperty("ci.lang-code"),
                ZoneId.of(environment.getRequiredProperty("ci.zone-id")),
                environment.getProperty("ci.page-size", Integer.class, 100)
        );
    }
}
