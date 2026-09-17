package com.itmsg.device42.integration.d42maximo.asset.tcpip;

import com.itmsg.device42.maximo.asset.DpaTcpIpUpsert;
import com.itmsg.device42.maximo.asset.DpaTcpIpWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TcpIpImport {

    private static final Logger log = LoggerFactory.getLogger(TcpIpImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final TcpIpQuery query;
    private final TcpIpMapper mapper;
    private final DpaTcpIpWriter writer;

    public TcpIpImport(TcpIpQuery query, TcpIpMapper mapper, DpaTcpIpWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA TCP/IP 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<IpAddressSource> data = query.getData(offset, limit);

            List<DpaTcpIpUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
