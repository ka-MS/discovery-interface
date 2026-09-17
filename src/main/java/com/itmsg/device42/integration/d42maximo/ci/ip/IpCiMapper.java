package com.itmsg.device42.integration.d42maximo.ci.ip;

import com.itmsg.device42.dto.device42.ci.IpSource;
import com.itmsg.device42.dto.maximo.ci.ActCiSpecUpsert;
import com.itmsg.device42.dto.maximo.ci.ActCiUpsert;
import com.itmsg.device42.dto.maximo.ci.CiUpsert;
import com.itmsg.device42.dto.maximo.ci.ClassificationDefinition;
import com.itmsg.device42.enums.ci.CiClassification;
import com.itmsg.device42.enums.ci.IpSpec;
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
public class IpCiMapper {

    public IpCiMapper(CiSpecMapper specMapper) {
        this.specMapper = specMapper;
    }

    private static final Logger log = LoggerFactory.getLogger(IpCiMapper.class);

    private static final String CHANGE_BY = "Device42";

    private static final String LANG_CODE = "KO";

    private final CiSpecMapper specMapper;

    public List<CiUpsert> mapData(List<IpSource> data, CiDefinitionCache definitions) {
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
                specMapper.addSpec(specs, definitions, actCi, IpSpec.STRING_NOTATION, source.ipAddress(), null);
                specMapper.addSpec(specs, definitions, actCi, IpSpec.MANAGED_SYSTEM_NAME, source.deviceName(), null);
                specMapper.addSpec(specs, definitions, actCi, IpSpec.LABEL, source.label(), null);

                mappedData.add(new CiUpsert(actCi, List.copyOf(specs)));
            } catch (RuntimeException e) {
                log.error("IP 매핑에 실패했습니다. ipAddressPk={}", source.ipAddressPk(), e);
            }
        }
        return mappedData;
    }
}
