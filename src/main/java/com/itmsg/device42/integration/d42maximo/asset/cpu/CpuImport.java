package com.itmsg.device42.integration.d42maximo.asset.cpu;

import com.itmsg.device42.maximo.asset.DpaCpuUpsert;
import com.itmsg.device42.maximo.asset.DpaCpuWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CpuImport {

    private static final Logger log = LoggerFactory.getLogger(CpuImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final CpuQuery query;
    private final CpuMapper mapper;
    private final DpaCpuWriter writer;

    public CpuImport(CpuQuery query, CpuMapper mapper, DpaCpuWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA CPU 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<ProcessorSource> data = query.getData(offset, limit);

            List<DpaCpuUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
