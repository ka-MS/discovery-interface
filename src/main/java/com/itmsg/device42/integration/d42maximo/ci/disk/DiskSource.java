package com.itmsg.device42.integration.d42maximo.ci.disk;

import java.math.BigDecimal;

/** Computer에 장착된 디스크 파트 한 건. 발견 시각은 부모 Computer에서 가져온다. */
public record DiskSource(
        long partPk, long devicePk, String model, String serialNo, String description,
        BigDecimal size, String sizeUnit, String lastDiscovered
) {
}
