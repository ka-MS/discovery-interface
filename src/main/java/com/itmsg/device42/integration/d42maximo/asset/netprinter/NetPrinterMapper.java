package com.itmsg.device42.integration.d42maximo.asset.netprinter;

import com.itmsg.device42.maximo.asset.DpaNetPrinterUpsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NetPrinterMapper {

    public List<DpaNetPrinterUpsert> mapData(List<NetworkPrinterSource> sourceData) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaNetPrinterUpsert> mappedData = new ArrayList<>(sourceData.size());

        for (NetworkPrinterSource source : sourceData) {
            mappedData.add(new DpaNetPrinterUpsert(
                    source.devicePk().longValue(),
                    roundCurrentRam(source.currentRam()),
                    source.macAddress(),
                    source.networkAddress(),
                    source.trayCount(),
                    source.ramUnit(),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static BigDecimal roundCurrentRam(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
