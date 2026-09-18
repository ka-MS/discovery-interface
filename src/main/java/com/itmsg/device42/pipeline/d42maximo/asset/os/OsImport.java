package com.itmsg.device42.pipeline.d42maximo.asset.os;

import com.itmsg.device42.source.device42.asset.os.OperatingSystemSource;
import com.itmsg.device42.source.device42.asset.os.OsQuery;

import com.itmsg.device42.target.maximo.asset.DpaOsUpsert;
import com.itmsg.device42.target.maximo.asset.DpaOsWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OsImport {

    private static final Logger log = LoggerFactory.getLogger(OsImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final OsQuery query;
    private final OsMapper mapper;
    private final DpaOsWriter writer;

    public OsImport(OsQuery query, OsMapper mapper, DpaOsWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA OS 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<OperatingSystemSource> data = query.getData(offset, limit);

            List<DpaOsUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
