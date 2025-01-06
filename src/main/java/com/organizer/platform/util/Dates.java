package com.organizer.platform.util;

import org.joda.time.*;
import org.springframework.lang.Nullable;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Utility class for handling date and time operations.
 * This class provides methods for converting between different date-time representations
 * and performs timezone-aware operations using JodaTime library.
 * It serves as a wrapper to simplify common date operations and maintain consistency
 * across the application.
 */
public class Dates {
    /** Date format pattern for short date representation (YYYY-MM-DD) */
    public static SimpleDateFormat shortDate = new SimpleDateFormat("yyyy-MM-dd");

    /** Default timezone for the application set to Asia/Jerusalem */
    public static TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Jerusalem");

    public Dates() {
    }

    /**
     * Converts a LocalDate object to its string representation.
     * @param date The LocalDate to convert, can be null
     * @return String representation of the date in YYYY-MM-DD format, or null if input is null
     */
    public static String dateToStr(@Nullable LocalDate date) {
        return date == null ? null : shortDate.format(date);
    }

    /**
     * Converts a LocalDateTime to a Date object using the default timezone.
     * @param date LocalDateTime to convert
     * @return Date object representing the same instant in UTC
     */
    public static Date atUtc(LocalDateTime date) {
        return atUtc(date, TIME_ZONE);
    }

    /**
     * Converts a LocalDateTime to a Date object using the specified timezone.
     * This method handles the conversion of local time to UTC while preserving
     * the correct instant across different timezones.
     *
     * @param date LocalDateTime to convert
     * @param zone TimeZone to use for the conversion
     * @return Date object representing the same instant in UTC, null if input is null
     */
    public static Date atUtc(LocalDateTime date, TimeZone zone) {
        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.setTimeZone(zone);
        calendar.set(date.getYear(), date.getMonthOfYear()-1, date.getDayOfMonth());
        calendar.set(Calendar.HOUR_OF_DAY, date.getHourOfDay());
        calendar.set(Calendar.MINUTE, date.getMinuteOfHour());
        calendar.set(Calendar.SECOND, date.getSecondOfMinute());
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * Converts a LocalDate to a Date object using the default timezone.
     * The time component is set to midnight.
     *
     * @param date LocalDate to convert
     * @return Date object representing midnight on the given date in UTC
     */
    public static Date atUtc(@Nullable LocalDate date) {
        return atUtc(date, TIME_ZONE);
    }

    /**
     * Converts a LocalDate to a Date object using the specified timezone.
     * The time component is set to midnight in the specified timezone.
     *
     * @param date LocalDate to convert
     * @param zone TimeZone to use for the conversion
     * @return Date object representing midnight on the given date in UTC
     */
    public static Date atUtc(@Nullable LocalDate date, TimeZone zone) {
        return date == null ? null : atUtc(date.toLocalDateTime(LocalTime.MIDNIGHT), zone);
    }

    /**
     * Converts a Date object to LocalDateTime using the default timezone.
     *
     * @param date Date object to convert
     * @return LocalDateTime representation in the default timezone
     */
    public static LocalDateTime atLocalTime(Date date) {
        return atLocalTime(date, TIME_ZONE);
    }

    /**
     * Converts a Date object to LocalDateTime using the specified timezone.
     * This method properly handles the conversion from UTC to local time,
     * ensuring that the correct instant is represented in the target timezone.
     *
     * @param date Date object to convert
     * @param zone TimeZone to use for the conversion
     * @return LocalDateTime representation in the specified timezone
     */
    public static LocalDateTime atLocalTime(Date date, TimeZone zone) {
        if (date == null) return null;
        var localDate = OffsetDateTime.ofInstant(date.toInstant(), zone.toZoneId()).toLocalDateTime();
        Calendar c = Calendar.getInstance();
        c.set(localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth());
        c.set(Calendar.HOUR_OF_DAY, localDate.getHour());
        c.set(Calendar.MINUTE, localDate.getMinute());
        c.set(Calendar.SECOND, localDate.getSecond());
        c.set(Calendar.MILLISECOND, 0);
        return LocalDateTime.fromCalendarFields(c);
    }

    /**
     * Gets the current UTC time as a Date object.
     *
     * @return Current UTC time as a Date object
     */
    public static Date nowUTC() {
        return DateTime.now().withZone(DateTimeZone.UTC).toDate();
    }

    /**
     * Gets the current UTC time as an ISO-formatted string.
     *
     * @return Current UTC time in ISO format
     */
    public static String getFullDateTime() {
        return DateTime.now().withZone(DateTimeZone.UTC).toDateTimeISO().toString();
    }

    /**
     * Compares two Date objects for equality, handling null values.
     * Two dates are considered equal if they represent the same instant in time
     * (same milliseconds since epoch) or if both are null.
     *
     * @param date1 First date to compare
     * @param date2 Second date to compare
     * @return true if dates are equal or both null, false otherwise
     */
    public static boolean equals(@Nullable Date date1, @Nullable Date date2) {
        if (date1 != null && date2 != null) {
            return date1.getTime() == date2.getTime();
        } else {
            return Objects.equals(date1, date2);
        }
    }
}