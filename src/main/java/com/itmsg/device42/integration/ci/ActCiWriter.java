package com.itmsg.device42.integration.ci;

import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** ACTCI·ACTCISPEC 쓰기를 전담한다. 모든 CI 유형이 공유한다. */
@Component
public class ActCiWriter {
    private static final Logger log = LoggerFactory.getLogger(ActCiWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public ActCiWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    /** @return ACTCI 적재에 성공한 건수. 실패·건너뛴 건은 각각 로그로 남는다. */
    public int write(List<CiUpsert> data) {
        int loaded = 0;

        for (CiUpsert ci : data) {
            ActCiUpsert actCi = ci.actCi();
            Long actCiId;
            try {
                actCiId = putActCi(actCi);
            } catch (DataAccessException e) {
                log.error("ACTCI 적재에 실패했습니다. actCiNum={}", actCi.actCiNum(), e);
                continue;
            }
            if (actCiId == null) {
                continue;
            }
            loaded++;

            for (ActCiSpecUpsert spec : ci.specs()) {
                try {
                    putActCiSpec(spec, actCiId);
                } catch (DataAccessException e) {
                    log.error("ACTCISPEC 적재에 실패했습니다. actCiNum={}, attribute={}",
                            spec.actCiNum(), spec.assetAttrId(), e);
                }
            }
        }
        return loaded;
    }

    private Long putActCi(ActCiUpsert ci) {
        List<ExistingCi> existing = maximoJdbcTemplate.query(
                FIND_ACTCI_QUERY,
                (rs, row) -> new ExistingCi(rs.getLong(1), rs.getString(2)), ci.actCiNum());
        if (!existing.isEmpty()) {
            ExistingCi old = existing.getFirst();
            if (!ci.classStructureId().equals(old.classStructureId())) {
                log.warn("분류 변경 규칙이 없어 건너뜁니다. actCiNum={}", ci.actCiNum());
                return null;
            }
            maximoJdbcTemplate.update(UPDATE_ACTCI_QUERY, ci.actCiName(), ci.description(), ci.lastScanDate(),
                    ci.changeBy(), ci.changeDate(), ci.langCode(), old.id());
            return old.id();
        }
        long id = maximoJdbcTemplate.queryForObject(NEXT_ACTCI_ID_QUERY, Long.class);
        maximoJdbcTemplate.update(INSERT_ACTCI_QUERY, id, ci.actCiNum(), ci.actCiName(), ci.classStructureId(),
                ci.description(), ci.lastScanDate(), ci.changeBy(), ci.changeDate(), ci.langCode());
        return id;
    }

    private void putActCiSpec(ActCiSpecUpsert spec, long actCiId) {
        maximoJdbcTemplate.update(MERGE_ACTCISPEC_QUERY,
                spec.actCiNum(), spec.assetAttrId(), spec.section(), spec.classStructureId(),
                spec.classSpecId(), actCiId, spec.displaySequence(), spec.mandatory() ? 1 : 0,
                spec.measureUnitId(), spec.linkedToAttribute(), spec.linkedToSection(),
                spec.alnValue(), spec.numValue(), spec.changeBy(), spec.changeDate());
    }

    private record ExistingCi(long id, String classStructureId) {
    }

    private static final String FIND_ACTCI_QUERY = "SELECT ACTCIID,CLASSSTRUCTUREID FROM MAXIMO.ACTCI WHERE ACTCINUM=?";

    private static final String UPDATE_ACTCI_QUERY = """
            UPDATE MAXIMO.ACTCI SET ACTCINAME=?,DESCRIPTION=?,LASTSCANDT=?,
                CHANGEBY=?,CHANGEDATE=?,LANGCODE=?
            WHERE ACTCIID=?
            """;

    private static final String INSERT_ACTCI_QUERY = """
            INSERT INTO MAXIMO.ACTCI
                (ACTCIID,ACTCINUM,ACTCINAME,CLASSSTRUCTUREID,DESCRIPTION,
                 LASTSCANDT,CHANGEBY,CHANGEDATE,LANGCODE,HASLD)
            VALUES (?,?,?,?,?,?,?,?,?,0)
            """;

    private static final String NEXT_ACTCI_ID_QUERY = "VALUES NEXT VALUE FOR MAXIMO.ACTCISEQ";

    /**
     * 파라미터 마커에 CAST가 필요하다. USING 절의 마커는 DB2가 타입을 추론하지 못해
     * SQLCODE=-418로 거부한다. ACTCISPECID는 NOT MATCHED 분기에서만 채번한다.
     * USING 절에서는 NEXT VALUE FOR가 금지된다(SQLCODE=-348).
     */
    private static final String MERGE_ACTCISPEC_QUERY = """
            MERGE INTO MAXIMO.ACTCISPEC AS target
            USING (VALUES (
                CAST(? AS VARCHAR(150)), CAST(? AS VARCHAR(300)), CAST(? AS VARCHAR(10)),
                CAST(? AS VARCHAR(25)), CAST(? AS BIGINT), CAST(? AS BIGINT),
                CAST(? AS INTEGER), CAST(? AS INTEGER), CAST(? AS VARCHAR(16)),
                CAST(? AS VARCHAR(300)), CAST(? AS VARCHAR(10)), CAST(? AS VARCHAR(254)),
                CAST(? AS DECIMAL(30,10)), CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
            )) AS source (
                ACTCINUM, ASSETATTRID, SECTION, CLASSSTRUCTUREID, CLASSSPECID,
                REFOBJECTID, DISPLAYSEQUENCE, MANDATORY, MEASUREUNITID,
                LINKEDTOATTRIBUTE, LINKEDTOSECTION, ALNVALUE, NUMVALUE, CHANGEBY, CHANGEDATE
            )
            ON target.ACTCINUM = source.ACTCINUM
                AND target.ASSETATTRID = source.ASSETATTRID
                AND (target.SECTION = source.SECTION
                     OR (target.SECTION IS NULL AND source.SECTION IS NULL))
            WHEN MATCHED THEN
                UPDATE SET
                    CLASSSTRUCTUREID = source.CLASSSTRUCTUREID,
                    CLASSSPECID = source.CLASSSPECID,
                    REFOBJECTID = source.REFOBJECTID,
                    REFOBJECTNAME = 'ACTCI',
                    DISPLAYSEQUENCE = source.DISPLAYSEQUENCE,
                    MANDATORY = source.MANDATORY,
                    MEASUREUNITID = source.MEASUREUNITID,
                    LINKEDTOATTRIBUTE = source.LINKEDTOATTRIBUTE,
                    LINKEDTOSECTION = source.LINKEDTOSECTION,
                    ALNVALUE = source.ALNVALUE,
                    NUMVALUE = source.NUMVALUE,
                    TABLEVALUE = NULL,
                    CHANGEBY = source.CHANGEBY,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ACTCISPECID, ACTCINUM, ASSETATTRID, CLASSSTRUCTUREID, CLASSSPECID,
                    SECTION, REFOBJECTID, REFOBJECTNAME, DISPLAYSEQUENCE, MANDATORY,
                    MEASUREUNITID, LINKEDTOATTRIBUTE, LINKEDTOSECTION, ALNVALUE,
                    NUMVALUE, TABLEVALUE, CHANGEBY, CHANGEDATE
                ) VALUES (
                    NEXT VALUE FOR MAXIMO.ACTCISPECSEQ,
                    source.ACTCINUM, source.ASSETATTRID, source.CLASSSTRUCTUREID,
                    source.CLASSSPECID, source.SECTION, source.REFOBJECTID, 'ACTCI',
                    source.DISPLAYSEQUENCE, source.MANDATORY, source.MEASUREUNITID,
                    source.LINKEDTOATTRIBUTE, source.LINKEDTOSECTION, source.ALNVALUE,
                    source.NUMVALUE, NULL, source.CHANGEBY, source.CHANGEDATE
                )
            """;
}
