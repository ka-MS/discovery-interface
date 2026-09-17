package com.itmsg.device42.integration.d42maximo.asset.disk;

import com.itmsg.device42.dto.device42.asset.DiskSource;
import com.itmsg.device42.dto.maximo.asset.DpaDiskUpsert;
import com.itmsg.device42.integration.asset.AssetIntegrationTask;
import com.itmsg.device42.maximo.asset.DpaDiskWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dpaDiskIntegrate")
@Order(6)
public class DiskImport implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DiskImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DiskQuery query;
    private final DiskMapper mapper;
    private final DpaDiskWriter writer;

    public DiskImport(DiskQuery query, DiskMapper mapper, DpaDiskWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    @Override
    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA Disk 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<DiskSource> data = query.getData(offset, limit);

            List<DpaDiskUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
