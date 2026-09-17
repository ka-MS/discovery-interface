package com.itmsg.device42.integration.d42maximo.ci.filesystem;

import com.itmsg.device42.integration.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.integration.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.integration.d42maximo.ci.mapping.FilesystemSpec;
import com.itmsg.device42.integration.d42maximo.ci.mapping.SourceTimestamp;
import com.itmsg.device42.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.maximo.ci.ActCiUpsert;
import com.itmsg.device42.maximo.ci.CiUpsert;
import com.itmsg.device42.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.maximo.ci.definition.ClassificationDefinition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FilesystemCiMapper {

    private static final Logger log = LoggerFactory.getLogger(FilesystemCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    /** 용량 단위 컬럼이 원천에 없다. 표본상 MB이며 근거는 원천 조사 문서에 있다. */
    private static final String CAPACITY_UNIT = "MBYTE";

    private final CiSpecMapper specMapper;

    public FilesystemCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    public List<CiUpsert> mapData(List<FilesystemSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (FilesystemSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.FILE_SYSTEM.classificationId());
                if (definition == null) {
                    log.warn("Filesystem 분류가 없어 건너뜁니다. mountPointPk={}", source.mountPointPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        "D42:MOUNTPOINT:" + source.mountPointPk(), source.mountPoint(), definition.classStructureId(),
                        null, SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.MOUNT_POINT, source.mountPoint(), null);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.TYPE, source.type(), null);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.CAPACITY, source.capacity(), CAPACITY_UNIT);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.AVAILABLE_SPACE, source.freeCapacity(), CAPACITY_UNIT);
                specMapper.addSpec(specs, definitions, actCi, FilesystemSpec.LABEL, source.label(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("Filesystem 매핑에 실패했습니다. mountPointPk={}", source.mountPointPk(), e);
            }
        }
        return mappedData;
    }
}
