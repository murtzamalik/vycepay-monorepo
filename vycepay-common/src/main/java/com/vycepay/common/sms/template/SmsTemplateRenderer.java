package com.vycepay.common.sms.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {@code {name}} placeholders; unknown tokens become empty; truncates to 640.
 */
public final class SmsTemplateRenderer {

    private static final int MAX_LEN = 640;
    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private SmsTemplateRenderer() {
    }

    public static String render(String body, Map<String, String> vars) {
        if (body == null || body.isBlank()) {
            return "";
        }
        Map<String, String> safe = vars != null ? vars : Map.of();
        Matcher m = TOKEN.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            String value = safe.getOrDefault(name, "");
            if (value == null) {
                value = "";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return truncate(sb.toString().replaceAll(" {2,}", " ").trim(), MAX_LEN);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
