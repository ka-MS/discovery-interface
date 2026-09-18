package com.itmsg.device42.pipeline.d42maximo.software.catalog;

import com.itmsg.device42.source.device42.software.catalog.SoftwareProductSource;

import com.itmsg.device42.pipeline.d42maximo.software.mapping.SoftwareIdentity;
import com.itmsg.device42.target.maximo.software.TloamSoftwareUpsert;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TloamSoftwareMapper {

    private static final String UNKNOWN = "UNKNOWN";

    public List<TloamSoftwareUpsert> mapData(List<SoftwareProductSource> data) {
        List<TloamSoftwareUpsert> mappedData = new ArrayList<>(data.size());

        for (SoftwareProductSource source : data) {
            String softwareName = defaultUnknown(source.softwareName());
            String manufacturer = defaultUnknown(source.manufacturer());
            String version = trimToNull(source.version());

            mappedData.add(new TloamSoftwareUpsert(
                    SoftwareIdentity.buildUniqueId(softwareName, version, manufacturer),
                    softwareName,
                    manufacturer,
                    version
            ));
        }

        return mappedData;
    }

    private static String defaultUnknown(String value) {
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
