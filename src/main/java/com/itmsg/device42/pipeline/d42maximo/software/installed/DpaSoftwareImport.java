package com.itmsg.device42.pipeline.d42maximo.software.installed;

import com.itmsg.device42.source.device42.software.installed.InstalledSoftwareQuery;
import com.itmsg.device42.source.device42.software.installed.InstalledSoftwareSource;

import com.itmsg.device42.target.maximo.software.DpaSoftwareUpsert;
import com.itmsg.device42.target.maximo.software.DpaSoftwareWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpaSoftwareImport {

    private static final Logger log = LoggerFactory.getLogger(DpaSoftwareImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final InstalledSoftwareQuery query;
    private final DpaSoftwareMapper mapper;
    private final DpaSoftwareWriter writer;

    public DpaSoftwareImport(InstalledSoftwareQuery query, DpaSoftwareMapper mapper, DpaSoftwareWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA 소프트웨어 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        log.info("배치할 총 데이터. totalcount={}", totalCount);

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<InstalledSoftwareSource> data = query.getData(offset, limit);

            List<DpaSoftwareUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
