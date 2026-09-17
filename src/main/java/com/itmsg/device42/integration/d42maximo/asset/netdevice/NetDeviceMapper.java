package com.itmsg.device42.integration.d42maximo.asset.netdevice;

import com.itmsg.device42.dto.device42.asset.NetworkDeviceSource;
import com.itmsg.device42.dto.maximo.asset.DpaNetDeviceUpsert;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NetDeviceMapper {

    public List<DpaNetDeviceUpsert> mapData(List<NetworkDeviceSource> sourceData) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaNetDeviceUpsert> mappedData = new ArrayList<>(sourceData.size());

        for (NetworkDeviceSource source : sourceData) {
            mappedData.add(new DpaNetDeviceUpsert(
                    source.devicePk().longValue(),
                    source.macAddress(),
                    source.networkAddress(),
                    source.osVersion(),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }
}
