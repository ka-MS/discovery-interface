package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** ACTCIRELATION 쓰기를 전담한다. 모든 관계 유형이 공유한다. */
@Component
public class ActCiRelationWriter {
    private static final Logger log = LoggerFactory.getLogger(ActCiRelationWriter.class);
    private static final String CHANGE_BY = "Device42";
    private static final int SWAPPED = 0;

    private final JdbcTemplate maximoJdbcTemplate;

    public ActCiRelationWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    /** @return 적재에 성공한 관계 건수. 걸러진 건과 실패한 건은 각각 로그로 남는다. */
    public int write(List<ActCiRelationUpsert> data) {
        // CiRelationJob이 관계 소스마다 페이지 단위로 write()를 호출하므로 이 시각은
        // 잡 전체의 단일 실행 시각이 아니라 페이지 단위 시각이다. 본체 task(OsCiIntegrate.mapData 등)도
        // 배치당 하나의 LocalDateTime.now()를 쓰는 같은 컨벤션이다.
        LocalDateTime changeDate = LocalDateTime.now();
        int loaded = 0;

        for (ActCiRelationUpsert relation : data) {
            try {
                int rows = maximoJdbcTemplate.update(MERGE_ACTCIRELATION_QUERY,
                        relation.sourceCiNum(), relation.targetCiNum(), relation.relationNum(),
                        SWAPPED, CHANGE_BY, changeDate);
                if (rows == 0) {
                    log.warn("관계를 건너뜁니다. 양 끝 CI가 없거나 실제 분류쌍의 규칙이 없습니다. {} -> {} ({})",
                            relation.sourceCiNum(), relation.targetCiNum(), relation.relationNum());
                    continue;
                }
                loaded++;
            } catch (DataAccessException e) {
                log.error("관계 적재에 실패했습니다. {} -> {} ({})",
                        relation.sourceCiNum(), relation.targetCiNum(), relation.relationNum(), e);
            }
        }
        return loaded;
    }

    /**
     * 파라미터 마커에 CAST가 필요하다. USING 절의 마커는 DB2가 타입을 추론하지 못해
     * SQLCODE=-418로 거부한다. USING 절에서는 NEXT VALUE FOR가 금지된다(SQLCODE=-348).
     * 예상 분류를 받지 않고 조인된 ACTCI 행의 실제 CLASSSTRUCTUREID로 규칙을 확인한다.
     * 그래서 도착이 물리·가상 Computer 어느 쪽이든 호출자 분기 없이 처리된다.
     * ANCESTORCI·BASELINEDATE·GUID는 UPDATE 절에 없다. 기존 값을 덮어쓰지 않는다.
     */
    private static final String MERGE_ACTCIRELATION_QUERY = """
            MERGE INTO MAXIMO.ACTCIRELATION AS target
            USING (
                SELECT input.SOURCECI, input.TARGETCI, input.RELATIONNUM,
                       input.SWAPPED, input.CHANGEBY, input.CHANGEDATE
                FROM (VALUES (
                    CAST(? AS VARCHAR(150)), CAST(? AS VARCHAR(150)),
                    CAST(? AS VARCHAR(192)), CAST(? AS INTEGER),
                    CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
                )) AS input (
                    SOURCECI, TARGETCI, RELATIONNUM, SWAPPED, CHANGEBY, CHANGEDATE
                )
                JOIN MAXIMO.ACTCI s ON s.ACTCINUM = input.SOURCECI
                JOIN MAXIMO.ACTCI t ON t.ACTCINUM = input.TARGETCI
                WHERE EXISTS (
                    SELECT 1 FROM MAXIMO.RELATIONRULES r
                    WHERE r.RELATIONNUM = input.RELATIONNUM
                      AND r.SOURCECLASS = s.CLASSSTRUCTUREID
                      AND r.TARGETCLASS = t.CLASSSTRUCTUREID
                )
                AND EXISTS (
                    SELECT 1 FROM MAXIMO.RELATION r
                    WHERE r.RELATIONNUM = input.RELATIONNUM
                )
            ) AS source
            ON target.SOURCECI = source.SOURCECI
                AND target.TARGETCI = source.TARGETCI
                AND target.RELATIONNUM = source.RELATIONNUM
            WHEN MATCHED THEN
                UPDATE SET
                    SWAPPED = source.SWAPPED,
                    CHANGEBY = source.CHANGEBY,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ACTCIRELATIONID, SOURCECI, TARGETCI, RELATIONNUM,
                    SWAPPED, CHANGEBY, CHANGEDATE
                ) VALUES (
                    NEXT VALUE FOR MAXIMO.ACTCIRELATIONSEQ,
                    source.SOURCECI, source.TARGETCI, source.RELATIONNUM,
                    source.SWAPPED, source.CHANGEBY, source.CHANGEDATE
                )
            """;
}
