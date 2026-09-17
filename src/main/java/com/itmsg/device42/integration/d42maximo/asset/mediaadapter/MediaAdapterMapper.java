package com.itmsg.device42.integration.d42maximo.asset.mediaadapter;

import com.itmsg.device42.maximo.asset.DpaMediaAdapterUpsert;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MediaAdapterMapper {

    private static final String UNKNOWN = "UNKNOWN";

    private static final String MEDIA_TYPE = "Video";

    public List<DpaMediaAdapterUpsert> mapData(List<MediaAdapterSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaMediaAdapterUpsert> mappedData = new ArrayList<>(data.size());

        for (MediaAdapterSource source : data) {
            mappedData.add(new DpaMediaAdapterUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    firstNonBlank(
                            source.description(),
                            source.modelDescription(),
                            source.modelName()
                    ),
                    defaultUnknown(source.modelName()),
                    defaultUnknown(source.vendorName()),
                    MEDIA_TYPE,
                    trimToNull(source.serialNo()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
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
