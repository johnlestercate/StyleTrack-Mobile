package com.styletrack.customer.ui.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Date/money/duration formatting helpers. Ported verbatim from the Compose Layout.kt. */
public final class Formats {
    private Formats() {}

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DAY_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter WIRE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static LocalDateTime parseDateTime(String s) {
        return LocalDateTime.parse(s);
    }

    public static String fmtDay(String s) {
        return parseDateTime(s).format(DAY_FMT);
    }

    public static String fmtLongDay(LocalDate d) {
        return d.format(LONG_DAY_FMT);
    }

    public static String fmtTime(String s) {
        return parseDateTime(s).format(TIME_FMT);
    }

    public static String fmtClock(LocalTime t) {
        return t.format(TIME_FMT);
    }

    public static String fmtWhen(String s) {
        LocalDateTime dt = parseDateTime(s);
        return dt.format(DAY_FMT) + ", " + dt.format(TIME_FMT);
    }

    public static String wireDateTime(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).format(WIRE_FMT);
    }

    public static String peso(double v) {
        if (v % 1.0 == 0.0) return String.format(Locale.US, "₱%,.0f", v);
        return String.format(Locale.US, "₱%,.2f", v);
    }

    public static String fmtDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        if (minutes % 60 == 0) return (minutes / 60) + " hr";
        return (minutes / 60) + " hr " + (minutes % 60) + " min";
    }

    public static String tierLabel(String tier) {
        if ("GOLD".equals(tier)) return "Gold member";
        if ("SILVER".equals(tier)) return "Silver member";
        return "Member";
    }
}
