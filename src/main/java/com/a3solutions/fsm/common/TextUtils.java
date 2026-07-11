package com.a3solutions.fsm.common;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.common
 * @project A3 Field Service Management Backend
 * @date 07/11/26
 */
public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean hasText(String value) {
        return !isBlank(value);
    }

    public static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
