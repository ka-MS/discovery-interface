package com.itmsg.device42.pipeline.d42maximo.asset.tcpip;

import com.itmsg.device42.source.device42.asset.tcpip.IpAddressSource;

import com.itmsg.device42.target.maximo.asset.DpaTcpIpUpsert;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TcpIpMapper {

    public List<DpaTcpIpUpsert> mapData(List<IpAddressSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaTcpIpUpsert> mappedData = new ArrayList<>(data.size());

        for (IpAddressSource source : data) {
            mappedData.add(new DpaTcpIpUpsert(
                    source.deviceFk(),
                    source.gateway(),
                    source.deviceName(),
                    source.ipAddress(),
                    toIpv4Netmask(source.maskBits()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static String toIpv4Netmask(Integer maskBits) {
        if (maskBits == null || maskBits == 0) {
            return null;
        }
        if (maskBits < 0 || maskBits > 32) {
            throw new IllegalArgumentException("IPv4 mask_bits 범위를 벗어났습니다: " + maskBits);
        }

        long mask = 0xFFFF_FFFFL << (32 - maskBits);
        return "%d.%d.%d.%d".formatted(
                (mask >>> 24) & 0xFF,
                (mask >>> 16) & 0xFF,
                (mask >>> 8) & 0xFF,
                mask & 0xFF
        );
    }
}
