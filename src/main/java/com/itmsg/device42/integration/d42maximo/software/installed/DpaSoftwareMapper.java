package com.itmsg.device42.integration.d42maximo.software.installed;

import com.itmsg.device42.integration.d42maximo.software.SoftwareIdentity;
import com.itmsg.device42.dto.device42.software.InstalledSoftwareSource;
import com.itmsg.device42.dto.maximo.software.DpaSoftwareUpsert;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DpaSoftwareMapper {

    private static final String UNKNOWN = "UNKNOWN";

    private static final long NO_SUITE = 0L;

    public List<DpaSoftwareUpsert> mapData(List<InstalledSoftwareSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaSoftwareUpsert> mappedData = new ArrayList<>(data.size());

        for (InstalledSoftwareSource source : data) {
            mappedData.add(new DpaSoftwareUpsert(
                    source.softwareInUsePk(),
                    source.deviceFk(),
                    defaultUnknown(source.softwareName()),
                    defaultUnknown(source.vendorName()),
                    trimToNull(source.version()),
                    SoftwareIdentity.buildUniqueId(
                            source.softwareName(),
                            source.version(),
                            source.vendorName()
                    ),
                    trimToNull(source.installPath()),
                    source.installDate(),
                    source.firstDetected(),
                    source.lastUpdated(),
                    NO_SUITE,
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
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
