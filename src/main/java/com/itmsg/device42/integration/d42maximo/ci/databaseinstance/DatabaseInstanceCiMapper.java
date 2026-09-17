package com.itmsg.device42.integration.d42maximo.ci.databaseinstance;

import com.itmsg.device42.dto.device42.ci.DatabaseInstanceSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.DatabaseInstanceSpec;
import com.itmsg.device42.integration.ci.CiDefinitionCache;
import com.itmsg.device42.integration.ci.CiSpecMapper;
import com.itmsg.device42.integration.ci.SourceTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInstanceCiMapper {

    public DatabaseInstanceCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    private static final Logger log = LoggerFactory.getLogger(DatabaseInstanceCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    private final CiSpecMapper specMapper;

    public List<CiUpsert> mapData(List<DatabaseInstanceSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (DatabaseInstanceSource source : data) {
            try {
                CiClassification classification = selectClassification(source.engine());
                ClassificationDefinition definition = definitions.classification(classification);
                if (definition == null) {
                    log.warn("DB Instance 분류가 없어 건너뜁니다. databaseInstancePk={}, classification={}",
                            source.databaseInstancePk(), classification);
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:DATABASEINSTANCE:" + source.databaseInstancePk(), source.name(),
                        definition.classStructureId(), source.description(),
                        SourceTimestamp.toLocalDateTime(source.lastChanged()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.NAME, source.name(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.PRODUCT_NAME, source.engine(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.PRODUCT_VERSION,
                        source.versionText(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.KEY_NAME, source.identifier(), null);
                specMapper.addSpec(specs, definitions, actCi, DatabaseInstanceSpec.HOME, source.installPath(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("DB Instance 매핑에 실패했습니다. databaseInstancePk={}",
                        source.databaseInstancePk(), e);
            }
        }
        return mappedData;
    }

    /**
     * 엔진 표기로 분류를 고른다. 전용 분류가 없는 엔진과 표기 없는 행은 범용 분류로 보낸다.
     * 엔진 문자열은 분류와 별개로 APPSERVER_PRODUCTNAME에 그대로 남는다.
     */
    static CiClassification selectClassification(String engine) {
        return switch (engine == null ? "" : engine.trim()) {
            case "Microsoft SQL" -> CiClassification.SQL_SERVER;
            case "DB2" -> CiClassification.DB2_INSTANCE;
            case "Oracle Database" -> CiClassification.ORACLE_INSTANCE;
            default -> CiClassification.DATABASE_SERVER;
        };
    }
}
