package com.itmsg.device42.integration.d42maximo.asset.netprinter;

import com.itmsg.device42.dto.device42.asset.NetworkPrinterSource;
import com.itmsg.device42.dto.maximo.asset.DpaNetPrinterUpsert;
import com.itmsg.device42.integration.asset.AssetIntegrationTask;
import com.itmsg.device42.maximo.asset.DpaNetPrinterWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dpaNetPrinterIntegrate")
@Order(4)
public class NetPrinterImport implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(NetPrinterImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final NetPrinterQuery query;
    private final NetPrinterMapper mapper;
    private final DpaNetPrinterWriter writer;

    public NetPrinterImport(NetPrinterQuery query, NetPrinterMapper mapper, DpaNetPrinterWriter writer) {
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
            List<NetworkPrinterSource> sourceData = query.getData(offset, limit);

            List<DpaNetPrinterUpsert> mappedData = mapper.mapData(sourceData);

            writer.write(mappedData);
        }
    }
}
