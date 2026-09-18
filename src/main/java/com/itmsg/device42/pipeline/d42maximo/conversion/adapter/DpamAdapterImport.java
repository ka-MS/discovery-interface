package com.itmsg.device42.pipeline.d42maximo.conversion.adapter;

import com.itmsg.device42.source.device42.conversion.adapter.AdapterModelSource;
import com.itmsg.device42.source.device42.conversion.adapter.DpamAdapterQuery;

import com.itmsg.device42.target.maximo.conversion.DpamAdapterUpsert;
import com.itmsg.device42.target.maximo.conversion.DpamAdapterWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpamAdapterImport {

    private static final Logger log = LoggerFactory.getLogger(DpamAdapterImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamAdapterQuery query;
    private final DpamAdapterWriter writer;

    public DpamAdapterImport(DpamAdapterQuery query, DpamAdapterWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 어댑터 변환 대상 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<AdapterModelSource> data = query.getData(offset, limit);

            List<DpamAdapterUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamAdapterUpsert> mapData(List<AdapterModelSource> data) {
        List<DpamAdapterUpsert> mappedData = new ArrayList<>(data.size());

        for (AdapterModelSource source : data) {
            String name = trimToNull(source.modelName());
            if (name != null) {
                mappedData.add(new DpamAdapterUpsert(name));
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
