package com.itmsg.device42.pipeline.d42maximo.conversion.manufacturer;

import com.itmsg.device42.source.device42.conversion.manufacturer.ManufacturerNamesQuery;
import com.itmsg.device42.source.device42.conversion.manufacturer.ManufacturerSource;

import com.itmsg.device42.target.maximo.conversion.DpamManuVariantUpsert;
import com.itmsg.device42.target.maximo.conversion.DpamManuVariantWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpamManuVariantImport {

    private static final Logger log = LoggerFactory.getLogger(DpamManuVariantImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final ManufacturerNamesQuery query;
    private final DpamManuVariantWriter writer;

    public DpamManuVariantImport(ManufacturerNamesQuery query, DpamManuVariantWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 제조업체 변환 변형 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<ManufacturerSource> data = query.getData(offset, limit);

            List<DpamManuVariantUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamManuVariantUpsert> mapData(List<ManufacturerSource> data) {
        List<DpamManuVariantUpsert> mappedData = new ArrayList<>(data.size());

        for (ManufacturerSource source : data) {
            String variant = trimToNull(source.vendorName());
            if (variant != null) {
                mappedData.add(new DpamManuVariantUpsert(variant, variant));
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
