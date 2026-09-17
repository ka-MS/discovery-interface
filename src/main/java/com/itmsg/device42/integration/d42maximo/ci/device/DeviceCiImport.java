package com.itmsg.device42.integration.d42maximo.ci.device;

import com.itmsg.device42.maximo.ci.ActCiWriter;
import com.itmsg.device42.maximo.ci.CiUpsert;
import com.itmsg.device42.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeviceCiImport {

    private static final Logger log = LoggerFactory.getLogger(DeviceCiImport.class);

    static final int DEFAULT_BATCH_SIZE = 1000;

    private final DeviceCiQuery query;
    private final DeviceCiMapper mapper;
    private final ActCiWriter writer;

    public DeviceCiImport(DeviceCiQuery query, DeviceCiMapper mapper, ActCiWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    public void integrate(CiDefinitionCache definitions) {
        long totalCount = query.getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 Device 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 Device 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (var page : PageLoop.pages(totalCount, DEFAULT_BATCH_SIZE)) {
            long offset = page.offset();
            int limit = page.limit();
            log.info("Device 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<DeviceSource> data = query.getData(offset, limit);
            List<CiUpsert> mappedData = mapper.mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += writer.write(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 Device가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("Device CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }
}
