package com.itmsg.device42.integration.d42maximo.software.installed;

import com.itmsg.device42.dto.device42.software.InstalledSoftwareSource;
import com.itmsg.device42.dto.maximo.software.DpaSoftwareUpsert;
import com.itmsg.device42.integration.software.SoftwareIntegrationTask;
import com.itmsg.device42.maximo.software.DpaSoftwareWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dpaSoftwareIntegrate")
@Order(2)
public class DpaSoftwareImport implements SoftwareIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaSoftwareImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpaSoftwareQuery query;
    private final DpaSoftwareMapper mapper;
    private final DpaSoftwareWriter writer;

    public DpaSoftwareImport(DpaSoftwareQuery query, DpaSoftwareMapper mapper, DpaSoftwareWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    @Override
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
