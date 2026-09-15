package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import com.itmsg.device42.integration.IntegrationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * CI 관계 적재. 본체·스펙 적재가 모두 끝난 뒤 실행한다.
 * 분류·속성 정의가 필요 없으므로 CiDefinitionCache를 받지 않는다.
 * 저장된 CI의 존재·분류·규칙만 보므로 본체 일부가 실패해도 진행한다.
 */
@Component("ci-relation")
public class CiRelationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(CiRelationJob.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiRelationWriter writer;

    public CiRelationJob(Device42ConnectionFactory connectionFactory, ActCiRelationWriter writer) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
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
        long totalCount = getTotalCount(source);
        if (totalCount <= 0) {
            log.info("배치할 {} 관계가 없습니다. totalCount={}", source, totalCount);
            return;
        }

        log.info("배치할 {} 관계 총 데이터. totalCount={}", source, totalCount);

        long readCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("{} 관계 배치를 조회합니다. offset={}, limit={}", source, offset, limit);

            List<ActCiRelationUpsert> data = getData(source, offset, limit);
            readCount += data.size();
            loadedCount += writer.write(data);
        }

        if (loadedCount < readCount) {
            log.warn("적재되지 않은 {} 관계가 있습니다. 조회={}, 적재={}", source, readCount, loadedCount);
        }

        log.info("{} 관계 적재를 마쳤습니다. 원천={}, 조회={}, 적재={}",
                source, totalCount, readCount, loadedCount);
    }

    private long getTotalCount(CiRelationSource source) {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(source.countQuery())) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException(source + " 관계 건수 조회에 실패했습니다.", e);
        }
    }

    private List<ActCiRelationUpsert> getData(CiRelationSource source, long offset, int limit) {
        String query = source.pageQuery(offset, limit);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<ActCiRelationUpsert> data = new ArrayList<>(limit);
            while (rs.next()) {
                data.add(new ActCiRelationUpsert(
                        rs.getString("sourceci"), rs.getString("targetci"), source.relationNum()));
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    source + " 관계 조회에 실패했습니다. offset=" + offset, e);
        }
    }
}
