package com.itmsg.device42.integration.d42maximo.conversion.procvariant;

import java.util.ArrayList;
import com.itmsg.device42.dto.device42.conversion.ProcessorModelSource;
import com.itmsg.device42.dto.maximo.conversion.DpamProcVariantUpsert;
import com.itmsg.device42.integration.conversion.ConversionIntegrationTask;
import com.itmsg.device42.maximo.conversion.DpamProcVariantWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dpamProcVariantIntegrate")
@Order(6)
public class DpamProcVariantImport implements ConversionIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpamProcVariantImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DpamProcVariantQuery query;
    private final DpamProcVariantWriter writer;

    public DpamProcVariantImport(DpamProcVariantQuery query, DpamProcVariantWriter writer) {
        this.query = query;
        this.writer = writer;
    }

    @Override
    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 프로세서 변환 변형 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<ProcessorModelSource> data = query.getData(offset, limit);

            List<DpamProcVariantUpsert> mappedData = mapData(data);

            writer.write(mappedData);
        }
    }

    private List<DpamProcVariantUpsert> mapData(List<ProcessorModelSource> data) {
        List<DpamProcVariantUpsert> mappedData = new ArrayList<>(data.size());

        for (ProcessorModelSource source : data) {
            String variant = trimToNull(source.modelName());
            if (variant != null) {
                mappedData.add(new DpamProcVariantUpsert(variant, variant));
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
