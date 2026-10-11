package com.styletrack.customer.data;

/** Mobile numbers are digits only; +63 917... and 0917... are the same number (matches the server). */
public final class Phones {
    private Phones() {}

    public static String normalizePhone(String raw) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) digits.append(c);
        }
        String d = digits.toString();
        if (d.length() == 12 && d.startsWith("63")) {
            return "0" + d.substring(2);
        }
        return d;
    }
}
