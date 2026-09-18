package com.itmsg.device42.pipeline.d42maximo.asset.logicaldrive;

import com.itmsg.device42.source.device42.asset.logicaldrive.LogicalDriveQuery;
import com.itmsg.device42.source.device42.asset.logicaldrive.LogicalDriveSource;

import com.itmsg.device42.target.maximo.asset.DpaLogicalDriveUpsert;
import com.itmsg.device42.target.maximo.asset.DpaLogicalDriveWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogicalDriveImport {

    private static final Logger log = LoggerFactory.getLogger(LogicalDriveImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final LogicalDriveQuery query;
    private final LogicalDriveMapper mapper;
    private final DpaLogicalDriveWriter writer;

    public LogicalDriveImport(LogicalDriveQuery query, LogicalDriveMapper mapper, DpaLogicalDriveWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA LogicalDrive 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<LogicalDriveSource> data = query.getData(offset, limit);

            List<DpaLogicalDriveUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
