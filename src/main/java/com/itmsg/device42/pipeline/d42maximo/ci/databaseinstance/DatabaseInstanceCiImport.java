package com.itmsg.device42.pipeline.d42maximo.ci.databaseinstance;

import com.itmsg.device42.source.device42.ci.databaseinstance.DatabaseInstanceCiQuery;
import com.itmsg.device42.source.device42.ci.databaseinstance.DatabaseInstanceSource;

import com.itmsg.device42.target.maximo.ci.ActCiWriter;
import com.itmsg.device42.target.maximo.ci.CiUpsert;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInstanceCiImport {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInstanceCiImport.class);

    static final int DEFAULT_BATCH_SIZE = 1000;

    private final DatabaseInstanceCiQuery query;
    private final DatabaseInstanceCiMapper mapper;
    private final ActCiWriter writer;

    public DatabaseInstanceCiImport(
            DatabaseInstanceCiQuery query,
            DatabaseInstanceCiMapper mapper,
            ActCiWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate(CiDefinitionCache definitions) {
        long totalCount = query.getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 DB Instance 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 DB Instance 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (var page : PageLoop.pages(totalCount, DEFAULT_BATCH_SIZE)) {
            long offset = page.offset();
            int limit = page.limit();
            log.info("DB Instance 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<DatabaseInstanceSource> data = query.getData(offset, limit);
            List<CiUpsert> mappedData = mapper.mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += writer.write(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 DB Instance가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("DB Instance CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }
}
