package com.itmsg.device42.dto.device42.ci;

/** 장비에 연결된 IP 주소 한 건. 네 유형 중 유일하게 자체 발견 시각이 있다. */
public record IpSource(
        long ipAddressPk, long devicePk, String ipAddress, String deviceName,
        String label, String notes, String lastDiscovered
) {
}
