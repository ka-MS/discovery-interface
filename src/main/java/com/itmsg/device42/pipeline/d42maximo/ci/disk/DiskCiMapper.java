package com.itmsg.device42.pipeline.d42maximo.ci.disk;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.MaximoCiIdentity;
import com.itmsg.device42.source.device42.ci.relation.Device42Entity;

import com.itmsg.device42.source.device42.ci.disk.DiskSource;

import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiSpecMapper;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.DiskSpec;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.SourceTimestamp;
import com.itmsg.device42.target.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.target.maximo.ci.ActCiUpsert;
import com.itmsg.device42.target.maximo.ci.CiUpsert;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.target.maximo.ci.definition.ClassificationDefinition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiskCiMapper {

    private static final Logger log = LoggerFactory.getLogger(DiskCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    private static final BigDecimal GB_PER_TB = BigDecimal.valueOf(1024);

    private final CiSpecMapper specMapper;

    public DiskCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    public List<CiUpsert> mapData(List<DiskSource> data, CiDefinitionCache definitions) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<CiUpsert> mappedData = new ArrayList<>(data.size());

        for (DiskSource source : data) {
            try {
                ClassificationDefinition definition = definitions.classification(CiClassification.DISK_DRIVE.classificationId());
                if (definition == null) {
                    log.warn("Disk 분류가 없어 건너뜁니다. partPk={}", source.partPk());
                    continue;
                }

                ActCiUpsert actCi = new ActCiUpsert(
                        MaximoCiIdentity.of(Device42Entity.PART, source.partPk()), source.model(), definition.classStructureId(),
                        source.description(), SourceTimestamp.toLocalDateTime(source.lastDiscovered()),
                        CHANGE_BY, applyDateTime, LANG_CODE);

                List<ActCiSpecUpsert> specs = new ArrayList<>();
                specMapper.addSpec(specs, definitions, actCi, DiskSpec.MODEL, source.model(), null);
                specMapper.addSpec(specs, definitions, actCi, DiskSpec.SERIAL_NUMBER, source.serialNo(), null);
                specMapper.addSpec(specs, definitions, actCi, DiskSpec.DISK_SIZE,
                        gigabytes(source.size(), source.sizeUnit()), sizeUnit(source.sizeUnit()));

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("Disk 매핑에 실패했습니다. partPk={}", source.partPk(), e);
            }
        }
        return mappedData;
    }

    /** MEASUREUNIT에 TBYTE가 없어 TB는 GB로 환산한다. 미지원 단위는 단위 코드가 없어 스펙이 생략된다. */
    private static BigDecimal gigabytes(BigDecimal size, String unit) {
        if (size == null) {
            return null;
        }
        return "TB".equals(unit == null ? "" : unit.trim()) ? size.multiply(GB_PER_TB) : size;
    }

    private static String sizeUnit(String unit) {
        return switch (unit == null ? "" : unit.trim()) {
            case "GB", "TB" -> "GBYTE";
            default -> null;
        };
    }
}
