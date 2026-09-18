package com.itmsg.device42.pipeline.d42maximo.asset.logicaldrive;

import com.itmsg.device42.source.device42.asset.logicaldrive.LogicalDriveSource;

import com.itmsg.device42.target.maximo.asset.DpaLogicalDriveUpsert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LogicalDriveMapper {

    private static final int FALSE = 0;

    private static final int VOLUME_LABEL_MAX_LENGTH = 16;

    private static final String DRIVE_TYPE = "UNKNOWN";

    private static final String SIZE_UNIT = "MB";

    public List<DpaLogicalDriveUpsert> mapData(List<LogicalDriveSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaLogicalDriveUpsert> mappedData = new ArrayList<>(data.size());

        for (LogicalDriveSource source : data) {
            mappedData.add(new DpaLogicalDriveUpsert(
                    source.mountPointPk(),
                    source.deviceFk(),
                    attachedNetworkName(source.fileSystem(), source.fileSystemType()),
                    roundSize(source.availableSize()),
                    FALSE,
                    DRIVE_TYPE,
                    FALSE,
                    trimToNull(source.fileSystemType()),
                    trimToNull(source.mountPoint()),
                    SIZE_UNIT,
                    roundSize(source.totalSize()),
                    truncate(source.volumeLabel(), VOLUME_LABEL_MAX_LENGTH),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    static BigDecimal roundSize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    static String attachedNetworkName(String fileSystem, String fileSystemType) {
        String normalizedType = trimToNull(fileSystemType);
        if (normalizedType == null) {
            return null;
        }

        String lowerType = normalizedType.toLowerCase(Locale.ROOT);
        if (!lowerType.equals("nfs") && !lowerType.equals("nfs4")) {
            return null;
        }

        return trimToNull(fileSystem);
    }

    static String truncate(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
