package com.itmsg.device42.integration.d42maximo.software;

import java.util.Locale;

public final class SoftwareIdentity {
    private SoftwareIdentity() {}

    private static final String UNKNOWN = "UNKNOWN";

    public static String buildUniqueId(String softwareName, String version, String manufacturer) {
        String nameToken = defaultUnknown(softwareName).toUpperCase(Locale.ROOT);
        String versionToken = defaultUnknown(version).replace(" ", "").toUpperCase(Locale.ROOT);
        String manufacturerToken = defaultUnknown(manufacturer).toUpperCase(Locale.ROOT);
        return nameToken + "|" + versionToken + "|" + manufacturerToken;
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
