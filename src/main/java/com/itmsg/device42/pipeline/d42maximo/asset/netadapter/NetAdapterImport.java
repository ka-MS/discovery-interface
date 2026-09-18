package com.itmsg.device42.pipeline.d42maximo.asset.netadapter;

import com.itmsg.device42.source.device42.asset.netadapter.NetAdapterQuery;
import com.itmsg.device42.source.device42.asset.netadapter.NetworkInterfaceSource;

import com.itmsg.device42.target.maximo.asset.DpaNetAdapterUpsert;
import com.itmsg.device42.target.maximo.asset.DpaNetAdapterWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NetAdapterImport {

    private static final Logger log = LoggerFactory.getLogger(NetAdapterImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final NetAdapterQuery query;
    private final NetAdapterMapper mapper;
    private final DpaNetAdapterWriter writer;

    public NetAdapterImport(NetAdapterQuery query, NetAdapterMapper mapper, DpaNetAdapterWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA 네트워크 어댑터 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<NetworkInterfaceSource> data = query.getData(offset, limit);

            List<DpaNetAdapterUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
