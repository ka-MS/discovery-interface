package com.itmsg.device42.pipeline.d42maximo.conversion.os;

import com.itmsg.device42.source.device42.conversion.os.DpamOsQuery;
import com.itmsg.device42.source.device42.conversion.os.OperatingSystemNameSource;

import com.itmsg.device42.target.maximo.conversion.DpamOsUpsert;
import com.itmsg.device42.target.maximo.conversion.DpamOsWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpamOsImport {

    private static final Logger log = LoggerFactory.getLogger(DpamOsImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamOsQuery query;
    private final DpamOsWriter writer;

    public DpamOsImport(DpamOsQuery query, DpamOsWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 운영체제 변환 대상 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<OperatingSystemNameSource> data = query.getData(offset, limit);

            List<DpamOsUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamOsUpsert> mapData(List<OperatingSystemNameSource> data) {
        List<DpamOsUpsert> mappedData = new ArrayList<>(data.size());

        for (OperatingSystemNameSource source : data) {
            String name = trimToNull(source.osName());
            if (name != null) {
                mappedData.add(new DpamOsUpsert(name));
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
