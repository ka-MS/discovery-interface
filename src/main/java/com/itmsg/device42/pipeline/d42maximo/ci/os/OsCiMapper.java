package com.itmsg.device42.pipeline.d42maximo.ci.os;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.MaximoCiIdentity;
import com.itmsg.device42.source.device42.ci.relation.Device42Entity;

import com.itmsg.device42.source.device42.ci.os.OsSource;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.OsSpec;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.SourceTimestamp;
import com.itmsg.device42.target.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.target.maximo.ci.ActCiUpsert;
import com.itmsg.device42.target.maximo.ci.CiUpsert;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.target.maximo.ci.definition.ClassificationDefinition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OsCiMapper {

    private static final Logger log = LoggerFactory.getLogger(OsCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    private final CiSpecMapper specMapper;

    public OsCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    public List<CiUpsert> mapData(List<OsSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (OsSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.OPERATING_SYSTEM.classificationId());
                if (definition == null) {
                    log.warn("OS 분류가 없어 건너뜁니다. deviceOsPk={}", source.deviceOsPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        MaximoCiIdentity.of(Device42Entity.DEVICEOS, source.deviceOsPk()), source.osName(), definition.classStructureId(),
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
