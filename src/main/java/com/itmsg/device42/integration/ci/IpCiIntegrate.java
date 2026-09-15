package com.itmsg.device42.integration.ci;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.ci.IpSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.IpSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Computer와 NET.IPADDRESS 사이에 RELATIONRULES가 없어 관계는 적재하지 않는다.
 * CDM 경로는 Computer → NET.IPINTERFACE → NET.IPADDRESS이며 결정 대기다. ISSUE-8.
 */
@Component
public class IpCiIntegrate implements CiIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(IpCiIntegrate.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CHANGE_BY = "Device42";
    private static final String LANG_CODE = "KO";

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiWriter writer;
    private final CiSpecMapper specMapper;

    public IpCiIntegrate(Device42ConnectionFactory connectionFactory, ActCiWriter writer, CiSpecMapper specMapper) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
        this.specMapper = specMapper;
    }

    @Override
    public void integrate(CiDefinitionCache definitions) {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            log.info("배치할 IP 데이터가 없습니다. totalCount={}", totalCount);
            return;
        }

        log.info("배치할 IP 총 데이터. totalCount={}", totalCount);

        long readCount = 0;
        long mappedCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("IP 배치를 조회합니다. offset={}, limit={}", offset, limit);

            List<IpSource> data = getData(offset, limit);
            List<CiUpsert> mappedData = mapData(data, definitions);

            readCount += data.size();
            mappedCount += mappedData.size();
            loadedCount += putData(mappedData);
        }

        if (mappedCount < readCount) {
            log.warn("매핑에서 제외된 IP가 있습니다. 조회={}, 매핑={}", readCount, mappedCount);
        }

        log.info("IP CI 적재를 마쳤습니다. 원천={}, 조회={}, 매핑={}, 적재={}",
                totalCount, readCount, mappedCount, loadedCount);
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(TOTAL_COUNT_QUERY)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("IP 건수 조회에 실패했습니다.", e);
        }
    }

    public List<IpSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<IpSource> data = new ArrayList<>(limit);
            while (rs.next()) {
                long ipAddressPk = rs.getLong("ipaddress_pk");
                try {
                    data.add(new IpSource(
                            ipAddressPk, rs.getLong("device_fk"), rs.getString("ip_address"),
                            rs.getString("notes"), rs.getString("last_discovered")));
                } catch (SQLException e) {
                    log.error("IP 원천 변환에 실패했습니다. ipAddressPk={}", ipAddressPk, e);
                }
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException("IP 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    List<CiUpsert> mapData(List<IpSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (IpSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.IP_ADDRESS);
                if (definition == null) {
                    log.warn("IP 분류가 없어 건너뜁니다. ipAddressPk={}", source.ipAddressPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:IPADDRESS:" + source.ipAddressPk(), source.ipAddress(), definition.classStructureId(),
                        source.notes(), SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, IpSpec.DOT_NOTATION, source.ipAddress(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("IP 매핑에 실패했습니다. ipAddressPk={}", source.ipAddressPk(), e);
            }
        }
        return mappedData;
    }

    public int putData(List<CiUpsert> data) {
        return writer.write(data);
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_ipaddress_v2 i
            WHERE EXISTS (
                SELECT 1 FROM view_device_v2 d
                WHERE d.device_pk = ANY(i.device_fks) AND
            """ + CiSourceFilter.COMPUTER + """
            )
            """;

    /** device_fks가 최대 3~7개라 조인이 같은 주소를 여러 행으로 만든다. DISTINCT ON으로 하나만 남긴다. */
    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT DISTINCT ON (i.ipaddress_pk)
                i.ipaddress_pk, c.device_pk AS device_fk,
                HOST(i.ip_address) AS ip_address,
                NULLIF(TRIM(i.notes), '') AS notes,
                i.last_discovered
            FROM view_ipaddress_v2 i
            JOIN computer c ON c.device_pk = ANY(i.device_fks)
            ORDER BY i.ipaddress_pk, c.device_pk
            LIMIT %d OFFSET %d
            """;
}
