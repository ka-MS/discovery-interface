package com.itmsg.device42.integration.d42maximo.asset.computer;

import com.itmsg.device42.maximo.asset.DpaComputerUpsert;
import com.itmsg.device42.maximo.asset.DpaComputerWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ComputerImport {

    private static final Logger log = LoggerFactory.getLogger(ComputerImport.class);

    private static final int DEFAULT_BATCH_SIZE = 10;

    private final ComputerQuery query;
    private final ComputerMapper mapper;
    private final DpaComputerWriter writer;

    public ComputerImport(ComputerQuery query, ComputerMapper mapper, DpaComputerWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0 || batchSize <= 0) {
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();
            List<ComputerHardwareSource> data = query.getData(offset, limit);

            List<DpaComputerUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
