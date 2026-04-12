package com.aura.finance.infrastructure.ai;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class AiMoneyFormatter {

    private AiMoneyFormatter() {
    }

    static String formatPhp(BigDecimal amount) {
        if (amount == null) {
            return "N/A";
        }

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return "₱" + formatter.format(amount);
    }

    static Map<String, String> formatPhpMap(Map<String, BigDecimal> amounts) {
        Map<String, String> formatted = new LinkedHashMap<>();
        amounts.forEach((key, value) -> formatted.put(key, formatPhp(value)));
        return formatted;
    }

    static String formatPhpMapInline(Map<String, BigDecimal> amounts) {
        return amounts.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + formatPhp(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
