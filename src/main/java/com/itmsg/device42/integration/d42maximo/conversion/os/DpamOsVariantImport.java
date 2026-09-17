package com.itmsg.device42.integration.d42maximo.conversion.os;

import com.itmsg.device42.maximo.conversion.DpamOsVariantUpsert;
import com.itmsg.device42.maximo.conversion.DpamOsVariantWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpamOsVariantImport {

    private static final Logger log = LoggerFactory.getLogger(DpamOsVariantImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamOsVariantQuery query;
    private final DpamOsVariantWriter writer;

    public DpamOsVariantImport(DpamOsVariantQuery query, DpamOsVariantWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 운영체제 변환 변형 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<OperatingSystemNameSource> data = query.getData(offset, limit);

            List<DpamOsVariantUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamOsVariantUpsert> mapData(List<OperatingSystemNameSource> data) {
        List<DpamOsVariantUpsert> mappedData = new ArrayList<>(data.size());

        for (OperatingSystemNameSource source : data) {
            String variant = trimToNull(source.osName());
            if (variant != null) {
                mappedData.add(new DpamOsVariantUpsert(variant, variant));
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
