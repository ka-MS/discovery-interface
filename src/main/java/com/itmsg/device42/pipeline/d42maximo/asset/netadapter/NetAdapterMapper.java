package com.itmsg.device42.pipeline.d42maximo.asset.netadapter;

import com.itmsg.device42.source.device42.asset.netadapter.NetworkInterfaceSource;

import com.itmsg.device42.target.maximo.asset.DpaNetAdapterUpsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class NetAdapterMapper {

    private static final String UNKNOWN = "UNKNOWN";

    private static final String ADAPTER_TYPE = "Network Adapter";

    private static final int PORT_MAX_LENGTH = 16;

    public List<DpaNetAdapterUpsert> mapData(List<NetworkInterfaceSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaNetAdapterUpsert> mappedData = new ArrayList<>(data.size());

        for (NetworkInterfaceSource source : data) {
            mappedData.add(new DpaNetAdapterUpsert(
                    source.netportPk(),
                    source.deviceFk(),
                    ADAPTER_TYPE,
                    parseBandwidth(source.portSpeed()),
                    parseBandwidthUnit(source.portSpeed()),
                    trimToNull(source.description()),
                    UNKNOWN,
                    defaultUnknown(source.vendorName()),
                    upperToNull(source.hwaddress()),
                    upperToNull(source.hwaddress2()),
                    truncate(source.port(), PORT_MAX_LENGTH),
                    trimToNull(source.globalType()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static BigDecimal parseBandwidth(String portSpeed) {
        String normalized = trimToNull(portSpeed);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(' ');
        String number = separator < 0 ? normalized : normalized.substring(0, separator);
        try {
            return new BigDecimal(number).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String parseBandwidthUnit(String portSpeed) {
        String normalized = trimToNull(portSpeed);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(' ');
        return separator < 0 ? null : trimToNull(normalized.substring(separator + 1));
    }

    static String upperToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    static String truncate(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    static String defaultUnknown(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? UNKNOWN : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
