package com.itmsg.device42.target.maximo.ci;

import java.time.LocalDateTime;

/** 숫자 ACTCIID는 저장 시 확보하고, 논리 식별자인 ACTCINUM으로 갱신한다. */
public record ActCiUpsert(
        String actCiNum, String actCiName, String classStructureId,
        String description, LocalDateTime lastScanDate, String changeBy,
        LocalDateTime changeDate, String langCode
) {
}
