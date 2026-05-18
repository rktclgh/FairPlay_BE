package com.fairing.fairplay.core.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

public final class TempUploadKeyPolicy {

    public static final String PRIVATE_TEMP_PREFIX = "private/tmp/";
    public static final String PUBLIC_UPLOAD_PREFIX = "uploads/";

    private static final String LEGACY_TEMP_DIRECTORY_PREFIX = "uploads/temp/";
    private static final Pattern LEGACY_TMP_DATE_PATTERN = Pattern.compile("^uploads/tmp\\d{4}-\\d{2}-\\d{2}/.+");

    private TempUploadKeyPolicy() {
    }

    public static String newPrivateTempPrefix() {
        return PRIVATE_TEMP_PREFIX + LocalDate.now() + "/";
    }

    public static boolean isTemporaryUploadKey(String key) {
        String normalizedKey = normalizeKey(key);
        return normalizedKey.startsWith(PRIVATE_TEMP_PREFIX)
                || normalizedKey.startsWith(LEGACY_TEMP_DIRECTORY_PREFIX)
                || LEGACY_TMP_DATE_PATTERN.matcher(normalizedKey).matches();
    }

    public static boolean isBlockedPublicUploadKey(String key) {
        return isTemporaryUploadKey(key);
    }

    public static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }

        String normalizedKey = key.replace('\\', '/');
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }
        return normalizedKey;
    }
}
