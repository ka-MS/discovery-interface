package com.itmsg.device42.integration.d42maximo.asset.netdevice;

import com.itmsg.device42.dto.device42.asset.NetworkDeviceSource;
import com.itmsg.device42.dto.maximo.asset.DpaNetDeviceUpsert;
import com.itmsg.device42.integration.asset.AssetIntegrationTask;
import com.itmsg.device42.maximo.asset.DpaNetDeviceWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dpaNetDeviceIntegrate")
@Order(3)
public class NetDeviceImport implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(NetDeviceImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final NetDeviceQuery query;
    private final NetDeviceMapper mapper;
    private final DpaNetDeviceWriter writer;

    public NetDeviceImport(NetDeviceQuery query, NetDeviceMapper mapper, DpaNetDeviceWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    @Override
    public void integrate() {
        long totalCount = query.getTotalCount();

        for (var page : PageLoop.pages(totalCount, DEFAULT_BATCH_SIZE)) {
            long offset = page.offset();
            int limit = page.limit();
            List<NetworkDeviceSource> sourceData = query.getData(offset, limit);

            List<DpaNetDeviceUpsert> mappedData = mapper.mapData(sourceData);

            writer.write(mappedData);
        }
    }
}
