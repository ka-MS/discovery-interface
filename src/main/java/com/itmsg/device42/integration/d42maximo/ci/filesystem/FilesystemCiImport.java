package com.itmsg.device42.integration.d42maximo.ci.filesystem;

import com.itmsg.device42.dto.device42.ci.FilesystemSource;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.integration.ci.ActCiWriter;
import com.itmsg.device42.integration.ci.CiDefinitionCache;
import com.itmsg.device42.integration.ci.CiIntegrationTask;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("filesystemCiIntegrate")
public class FilesystemCiImport implements CiIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(FilesystemCiImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final FilesystemCiQuery query;
    private final FilesystemCiMapper mapper;
    private final ActCiWriter writer;

    public FilesystemCiImport(FilesystemCiQuery query, FilesystemCiMapper mapper, ActCiWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = query.getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 Filesystem 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 Filesystem 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (var page : PageLoop.pages(totalCount, DEFAULT_BATCH_SIZE)) {
            long offset = page.offset();
            int limit = page.limit();
            log.info("Filesystem 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<FilesystemSource> data = query.getData(offset, limit);
            List<CiUpsert> mappedData = mapper.mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += writer.write(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 Filesystem이 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("Filesystem CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }
}
