package com.itmsg.device42.integration.d42maximo.conversion.manufacturer;

import com.itmsg.device42.maximo.conversion.DpamManufacturerUpsert;
import com.itmsg.device42.maximo.conversion.DpamManufacturerWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DpamManufacturerImport {

    private static final Logger log = LoggerFactory.getLogger(DpamManufacturerImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamManufacturerQuery query;
    private final DpamManufacturerWriter writer;

    public DpamManufacturerImport(DpamManufacturerQuery query, DpamManufacturerWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 제조업체 변환 대상 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<ManufacturerSource> data = query.getData(offset, limit);

            List<DpamManufacturerUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamManufacturerUpsert> mapData(List<ManufacturerSource> data) {
        List<DpamManufacturerUpsert> mappedData = new ArrayList<>(data.size());

        for (ManufacturerSource source : data) {
            String name = trimToNull(source.vendorName());
            if (name != null) {
                mappedData.add(new DpamManufacturerUpsert(name));
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
