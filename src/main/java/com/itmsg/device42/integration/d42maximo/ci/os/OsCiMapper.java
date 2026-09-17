package com.itmsg.device42.integration.d42maximo.ci.os;

import com.itmsg.device42.dto.device42.ci.OsSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.OsSpec;
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
public class OsCiMapper {

    public OsCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    private static final Logger log = LoggerFactory.getLogger(OsCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    private final CiSpecMapper specMapper;

    public List<CiUpsert> mapData(List<OsSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (OsSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.OPERATING_SYSTEM);
                if (definition == null) {
                    log.warn("OS 분류가 없어 건너뜁니다. deviceOsPk={}", source.deviceOsPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:DEVICEOS:" + source.deviceOsPk(), source.osName(), definition.classStructureId(),
                        null, SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, OsSpec.OS_NAME, source.osName(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.NAME, source.osName(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.OS_VERSION, source.osVersion(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.KERNEL_VERSION, source.kernelVersion(), null);
                specMapper.addSpec(specs, definitions, actCi, OsSpec.KERNEL_ARCHITECTURE, source.architecture(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("OS 매핑에 실패했습니다. deviceOsPk={}", source.deviceOsPk(), e);
            }
        }
        return mappedData;
    }
}
