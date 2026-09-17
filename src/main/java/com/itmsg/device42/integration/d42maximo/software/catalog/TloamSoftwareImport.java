package com.itmsg.device42.integration.d42maximo.software.catalog;

import com.itmsg.device42.dto.device42.software.SoftwareProductSource;
import com.itmsg.device42.dto.maximo.software.TloamSoftwareUpsert;
import com.itmsg.device42.integration.software.SoftwareIntegrationTask;
import com.itmsg.device42.maximo.software.TloamSoftwareWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("tloamSoftwareIntegrate")
@Order(1)
public class TloamSoftwareImport implements SoftwareIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(TloamSoftwareImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final TloamSoftwareQuery query;
    private final TloamSoftwareMapper mapper;
    private final TloamSoftwareWriter writer;

    public TloamSoftwareImport(TloamSoftwareQuery query, TloamSoftwareMapper mapper, TloamSoftwareWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    @Override
    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 TLOAM 소프트웨어 카탈로그 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        log.info("배치할 TLOAM 소프트웨어 카탈로그 총 데이터. totalcount={}", totalCount);

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<SoftwareProductSource> data = query.getData(offset, limit);

            List<TloamSoftwareUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
