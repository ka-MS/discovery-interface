package com.itmsg.device42.integration.d42maximo.asset.netdevice;

import com.itmsg.device42.maximo.asset.DpaNetDeviceUpsert;
import com.itmsg.device42.maximo.asset.DpaNetDeviceWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NetDeviceImport {

    private static final Logger log = LoggerFactory.getLogger(NetDeviceImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final NetDeviceQuery query;
    private final DpaNetDeviceWriter writer;

    public NetDeviceImport(NetDeviceQuery query, DpaNetDeviceWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();

        for (var page : PageLoop.pages(totalCount, DEFAULT_BATCH_SIZE)) {
            long offset = page.offset();
            int limit = page.limit();
            List<NetworkDeviceSource> sourceData = query.getData(offset, limit);

            List<DpaNetDeviceUpsert> mappedData = mapData(sourceData);

            writer.write(mappedData);
        }
    }

    private List<DpaNetDeviceUpsert> mapData(List<NetworkDeviceSource> sourceData) {
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
