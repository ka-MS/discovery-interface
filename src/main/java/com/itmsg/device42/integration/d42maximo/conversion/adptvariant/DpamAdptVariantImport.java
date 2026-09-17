package com.itmsg.device42.integration.d42maximo.conversion.adptvariant;

import java.util.ArrayList;
import com.itmsg.device42.dto.device42.conversion.AdapterModelSource;
import com.itmsg.device42.dto.maximo.conversion.DpamAdptVariantUpsert;
import com.itmsg.device42.integration.conversion.ConversionIntegrationTask;
import com.itmsg.device42.maximo.conversion.DpamAdptVariantWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dpamAdptVariantIntegrate")
@Order(8)
public class DpamAdptVariantImport implements ConversionIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpamAdptVariantImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamAdptVariantQuery query;
    private final DpamAdptVariantWriter writer;

    public DpamAdptVariantImport(DpamAdptVariantQuery query, DpamAdptVariantWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    @Override
    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 어댑터 변환 변형 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<AdapterModelSource> data = query.getData(offset, limit);

            List<DpamAdptVariantUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamAdptVariantUpsert> mapData(List<AdapterModelSource> data) {
        List<DpamAdptVariantUpsert> mappedData = new ArrayList<>(data.size());

        for (AdapterModelSource source : data) {
            String variant = trimToNull(source.modelName());
            if (variant != null) {
                mappedData.add(new DpamAdptVariantUpsert(variant, variant));
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
