package com.itmsg.device42.pipeline.d42maximo.ci.relation;

import com.itmsg.device42.source.device42.ci.relation.CiRelationQuery;

import com.itmsg.device42.target.maximo.ci.ActCiRelationWriter;
import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.PageLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CI 관계 적재. 본체·스펙 적재가 모두 끝난 뒤 실행한다.
 * 분류·속성 정의가 필요 없으므로 CiDefinitionCache를 받지 않는다.
 * 저장된 CI의 존재·분류·규칙만 보므로 본체 일부가 실패해도 진행한다.
 */
@Component("ci-relation")
public class CiRelationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(CiRelationJob.class);
    static final int DEFAULT_BATCH_SIZE = 1000;

    private final CiRelationQuery query;
    private final ActCiRelationWriter writer;
    private final CiRelationMapper mapper;

    public CiRelationJob(CiRelationQuery query, ActCiRelationWriter writer, CiRelationMapper mapper) {
        this.query = query;
        this.writer = writer;
        this.mapper = mapper;
    }

    @Override
    public void run() {
        for (CiRelationSource source : CiRelationSource.values()) {
            try {
                log.info("{} 관계 적재를 시작합니다.", source);
                integrate(source);
            } catch (Exception e) {
                log.error("{} 관계 적재에 실패했습니다. 다음 관계를 계속합니다.", source, e);
            }
        }
    }

    private void integrate(CiRelationSource source) {
        long totalCount = query.getTotalCount(source.source());
        if (totalCount <= 0) {
            log.info("배치할 {} 관계가 없습니다. totalCount={}", source, totalCount);
            return;
        }

        log.info("배치할 {} 관계 총 데이터. totalCount={}", source, totalCount);

        long readCount = 0;
        long loadedCount = 0;

        for (var page : PageLoop.pages(totalCount, DEFAULT_BATCH_SIZE)) {
            long offset = page.offset();
            int limit = page.limit();
            log.info("{} 관계 배치를 조회합니다. offset={}, limit={}", source, offset, limit);

            var data = query.getData(source.source(), offset, limit);
            readCount += data.size();
            loadedCount += writer.write(mapper.mapData(source, data));
        }

        if (loadedCount < readCount) {
            log.warn("적재되지 않은 {} 관계가 있습니다. 조회={}, 적재={}", source, readCount, loadedCount);
        }

        log.info("{} 관계 적재를 마쳤습니다. 원천={}, 조회={}, 적재={}",
                source, totalCount, readCount, loadedCount);
    }

}
