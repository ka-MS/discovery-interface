package com.itmsg.device42.integration.d42maximo.asset.mediaadapter;

import com.itmsg.device42.maximo.asset.DpaMediaAdapterUpsert;
import com.itmsg.device42.maximo.asset.DpaMediaAdapterWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MediaAdapterImport {

    private static final Logger log = LoggerFactory.getLogger(MediaAdapterImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final MediaAdapterQuery query;
    private final MediaAdapterMapper mapper;
    private final DpaMediaAdapterWriter writer;

    public MediaAdapterImport(MediaAdapterQuery query, MediaAdapterMapper mapper, DpaMediaAdapterWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA 미디어 어댑터 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<MediaAdapterSource> data = query.getData(offset, limit);

            List<DpaMediaAdapterUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
