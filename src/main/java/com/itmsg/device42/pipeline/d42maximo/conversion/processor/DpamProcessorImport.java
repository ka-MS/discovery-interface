package com.itmsg.device42.pipeline.d42maximo.conversion.processor;

import com.itmsg.device42.source.device42.conversion.processor.DpamProcessorQuery;
import com.itmsg.device42.source.device42.conversion.processor.ProcessorModelSource;

import com.itmsg.device42.target.maximo.conversion.DpamProcessorUpsert;
import com.itmsg.device42.target.maximo.conversion.DpamProcessorWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpamProcessorImport {

    private static final Logger log = LoggerFactory.getLogger(DpamProcessorImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamProcessorQuery query;
    private final DpamProcessorWriter writer;

    public DpamProcessorImport(DpamProcessorQuery query, DpamProcessorWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 프로세서 변환 대상 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<ProcessorModelSource> data = query.getData(offset, limit);

            List<DpamProcessorUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamProcessorUpsert> mapData(List<ProcessorModelSource> data) {
        List<DpamProcessorUpsert> mappedData = new ArrayList<>(data.size());

        for (ProcessorModelSource source : data) {
            String name = trimToNull(source.modelName());
            if (name != null) {
                mappedData.add(new DpamProcessorUpsert(name));
            }
        }

        return mappedData;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
