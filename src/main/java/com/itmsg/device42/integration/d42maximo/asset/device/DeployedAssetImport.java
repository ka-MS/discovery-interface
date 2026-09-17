package com.itmsg.device42.integration.d42maximo.asset.device;

import com.itmsg.device42.dto.device42.asset.DeviceSource;
import com.itmsg.device42.dto.maximo.asset.DeployedAssetUpsert;
import com.itmsg.device42.integration.asset.AssetIntegrationTask;
import com.itmsg.device42.maximo.asset.DeployedAssetWriter;
import com.itmsg.device42.runtime.PageLoop;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("deployedAssetIntegrate")
@Order(1)
public class DeployedAssetImport implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DeployedAssetImport.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DeployedAssetQuery query;
    private final DeployedAssetMapper mapper;
    private final DeployedAssetWriter writer;

    public DeployedAssetImport(DeployedAssetQuery query, DeployedAssetMapper mapper, DeployedAssetWriter writer) {
        this.query = query;
        this.mapper = mapper;
        this.writer = writer;
    }

    @Override
    public void integrate() {
        long totalCount = query.getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (var page : PageLoop.pages(totalCount, batchSize)) {
            long offset = page.offset();
            int limit = page.limit();

            List<DeviceSource> data = query.getData(offset, limit);

            List<DeployedAssetUpsert> mappedData = mapper.mapData(data);

            writer.write(mappedData);
        }
    }
}
