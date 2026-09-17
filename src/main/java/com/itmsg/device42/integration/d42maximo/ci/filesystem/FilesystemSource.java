package com.itmsg.device42.integration.d42maximo.ci.filesystem;

import java.math.BigDecimal;

/** Computer에 마운트된 파일시스템 한 건. 용량 단위는 원천에 없고 MB로 해석한다. */
public record FilesystemSource(
        long mountPointPk, long devicePk, String mountPoint, String type, String label,
        BigDecimal capacity, BigDecimal freeCapacity, String lastDiscovered
) {
}
