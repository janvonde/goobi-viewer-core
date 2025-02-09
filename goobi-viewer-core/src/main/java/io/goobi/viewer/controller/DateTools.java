/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Utility methods for date parsing, etc.
 */
public class DateTools {

    private static final Logger logger = LogManager.getLogger(DateTools.class);

    /** Constant <code>formatterISO8601Full</code> */
    public static final DateTimeFormatter formatterISO8601Full = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // yyyy-MM-dd'T'HH:mm:ss
    /** Constant <code>formatterISO8601DateTimeInstant</code> */
    public static final DateTimeFormatter formatterISO8601DateTimeInstant = DateTimeFormatter.ISO_INSTANT; // yyyy-MM-dd'T'HH:mm:ssZ
    /** Constant <code>formatterISO8601DateTimeWithOffset</code> */
    public static final DateTimeFormatter formatterISO8601DateTimeWithOffset = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // yyyy-MM-dd'T'HH:mm:ss+01:00
    /** Constant <code>formatterISO8601Date</code> */
    public static final java.time.format.DateTimeFormatter formatterISO8601Date = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    /** Constant <code>formatterISO8601Date</code> */
    public static final java.time.format.DateTimeFormatter formatterISO8601Time = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss
    /** Constant <code>formatterISO8601DateReverse</code> */
    public static final DateTimeFormatter formatterISO8601DateReverse = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // dd-MM-YYYY
    /** Constant <code>formatterISO8601YearMonth</code> */
    public static final DateTimeFormatter formatterISO8601YearMonth = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    /** Constant <code>formatterISO8601DateTime</code> */
    public static final DateTimeFormatter formatterISO8601DateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** Constant <code>formatterISO8601DateTimeMS</code> */
    public static final DateTimeFormatter formatterISO8601DateTimeMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    /** Constant <code>formatterDEDate</code> */
    public static final DateTimeFormatter formatterDEDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    /** Constant <code>formatterUSDate</code> */
    public static final DateTimeFormatter formatterENDate = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    /** Constant <code>formatterCNDate</code> */
    public static final DateTimeFormatter formatterCNDate = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    /** Constant <code>formatterJPDate</code> */
    public static final DateTimeFormatter formatterJPDate = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    /** Constant <code>formatterISO8601DateTimeNoSeconds</code> */
    public static final DateTimeFormatter formatterISO8601DateTimeNoSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** Constant <code>formatterDEDateTime</code> */
    public static final DateTimeFormatter formatterDEDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    /** Constant <code>formatterENDateTime</code> */
    public static final DateTimeFormatter formatterENDateTime = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a").withLocale(Locale.ENGLISH);
    /** Constant <code>formatterDEDateTimeNoSeconds</code> */
    public static final DateTimeFormatter formatterDEDateTimeNoSeconds = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    /** Constant <code>formatterENDateTimeNoSeconds</code> */
    public static final DateTimeFormatter formatterENDateTimeNoSeconds = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a").withLocale(Locale.ENGLISH);
    /** Constant <code>formatterISO8601BasicDateNoYear</code> */
    public static final DateTimeFormatter formatterISO8601BasicDateNoYear = new DateTimeFormatterBuilder()
            .appendPattern("MMdd")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    /** Constant <code>formatterISO8601BasicDate</code> */
    public static final DateTimeFormatter formatterISO8601BasicDate = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** Constant <code>formatterBasicDateTime</code> */
    public static final DateTimeFormatter formatterISO8601BasicDateTime = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** Constant <code>formatterJavaUtilDateToString</code> */
    public static final DateTimeFormatter formatterJavaUtilDateToString = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss zzz yyyy");

    public static final DateTimeFormatter formatterYearOnly = DateTimeFormatter.ofPattern("yyyy");

    public static final DateTimeFormatter formatterMonthDayOnly = DateTimeFormatter.ofPattern("MMdd");

    public static final DateTimeFormatter formatterFileName = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmssSSS");

    /**
     * 
     */
    private DateTools() {
        //
    }

    /**
     * Converts the given string to a list of Date objects created from the contents of this string (years or whole dates).
     *
     * @param dateString a {@link java.lang.String} object.
     * @should parse single date correctly
     * @should parse multiple dates correctly
     * @should parse dates in parentheses correctly
     * @return a {@link java.util.List} object.
     */
    public static List<LocalDateTime> parseMultipleDatesFromString(String dateString) {
        // logger.debug("Parsing date string : {}", dateString);
        if (StringUtils.isEmpty(dateString)) {
            return Collections.emptyList();
        }

        List<LocalDateTime> ret = new ArrayList<>();
        String splittingChar = "/";
        String[] dateStringSplit = dateString.split(splittingChar);
        for (String s : dateStringSplit) {
            s = s.trim();

            // Check whether this is a well-formed date and not a range or anything
            LocalDateTime date = parseDateFromString(s);
            if (date != null) {
                ret.add(date);
                continue;
            }

            // Try finding a complete date in the string (enclosed in parentheses)
            Pattern p = Pattern.compile(StringTools.REGEX_PARENTHESES);
            Matcher m = p.matcher(s);
            if (m.find()) {
                s = s.substring(m.start() + 1, m.end() - 1);
                logger.trace("Extracted date: {}", s);
                date = parseDateFromString(s);
                if (date != null) {
                    ret.add(date);
                    continue;
                }
            }

            // If no complete date was found, just use the year
            if (s.contains(" ")) {
                String[] sSplit = s.split(" ");
                s = sSplit[0];
            }
            try {
                int year = Integer.parseInt(s);
                ret.add(LocalDateTime.of(year, 1, 1, 0, 0));
            } catch (NumberFormatException e) {
                logger.error("Could not parse year: {}", s);
            }
        }

        return ret;
    }

    /**
     *
     * @param millis
     * @param utc
     * @return
     * @should create LocalDateTime correctly
     */
    public static LocalDateTime getLocalDateTimeFromMillis(long millis, boolean utc) {
        return Instant.ofEpochMilli(millis).atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     *
     * @param ldt
     * @param utc
     * @return
     */
    public static Long getMillisFromLocalDateTime(LocalDateTime ldt, boolean utc) {
        if (ldt == null) {
            return null;
        }

        ZonedDateTime zdt = ldt.atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    /**
     *
     * @param dateString
     * @param fromUTC
     * @return
     * @should parse iso date formats correctly
     * @should parse german date formats correctly
     * @should parse english date formats correctly
     * @should parse chinese date formats correctly
     * @should parse japanese date formats correctly
     * @should return null if unsupported format
     * @should throw IllegalArgumentException if dateString is null
     */
    public static LocalDateTime parseDateTimeFromString(String dateString, boolean fromUTC) {
        return parseDateTimeFromString(dateString, fromUTC, null);
    }

    /**
     * <p>
     * parseDateTimeFromString.
     * </p>
     *
     * @param dateString a {@link java.lang.String} object.
     * @param fromUTC a boolean.
     * @param zoneOffset
     * @return a {@link java.time.LocalDateTime} object.
     * @should parse iso date as UTC correctly
     */
    public static LocalDateTime parseDateTimeFromString(String dateString, boolean fromUTC, Integer zoneOffset) {
        if (dateString == null) {
            throw new IllegalArgumentException("dateString may not be null");
        }

        try {
            if (fromUTC) {
                ZoneId zoneId = zoneOffset != null ? ZoneId.ofOffset("UTC", ZoneOffset.ofHours(zoneOffset)) : ZoneId.systemDefault();
                return LocalDateTime.parse(dateString, formatterISO8601DateTimeInstant)
                        .atZone(zoneId)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            }
            return LocalDateTime.parse(dateString, formatterISO8601DateTimeInstant);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, formatterISO8601Full);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            if (fromUTC) {
                ZoneId zoneId = zoneOffset != null ? ZoneId.ofOffset("UTC", ZoneOffset.ofHours(zoneOffset)) : ZoneId.systemDefault();
                return LocalDateTime.parse(dateString, formatterISO8601DateTimeWithOffset)
                        .atZone(zoneId)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            }
            return LocalDateTime.parse(dateString, formatterISO8601DateTimeWithOffset);

        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, formatterISO8601DateTimeMS);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, formatterISO8601DateTime);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, formatterISO8601Date).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, formatterISO8601YearMonth).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, formatterDEDateTime);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, formatterENDateTime);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, formatterDEDate).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, formatterENDate).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, formatterJPDate).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, formatterCNDate).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }

        return null;
    }

    /**
     * <p>
     * parseDateFromString.
     * </p>
     *
     * @param dateString a {@link java.lang.String} object.
     * @return a {@link java.util.Date} object.
     */
    public static LocalDateTime parseDateFromString(String dateString) {
        return parseDateTimeFromString(dateString, false);
    }

    /**
     *
     * @param dateToConvert
     * @param utc
     * @return
     */
    public static Date convertLocalDateTimeToDateViaInstant(LocalDateTime dateToConvert, boolean utc) {
        if (dateToConvert == null) {
            return null;
        }
        return Date.from(dateToConvert.atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault()).toInstant());
    }

    /**
     *
     * @param dateToConvert
     * @return
     */
    public static LocalDateTime convertDateToLocalDateTimeViaInstant(Date dateToConvert) {
        if (dateToConvert == null) {
            return null;
        }
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     *
     * @param date java.util.Date
     * @param formatter
     * @param utc
     * @return
     */
    public static String format(Date date, DateTimeFormatter formatter, boolean utc) {
        if (date == null) {
            throw new IllegalArgumentException("date may not be null");
        }

        ZonedDateTime ld =
                convertDateToLocalDateTimeViaInstant(date).atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
        return ld.format(formatter);
    }

    /**
     *
     * @param localDateTime
     * @param formatter
     * @param utc
     * @return
     */
    public static String format(LocalDateTime localDateTime, DateTimeFormatter formatter, boolean utc) {
        if (localDateTime == null) {
            throw new IllegalArgumentException("localDateTime may not be null");
        }

        ZonedDateTime ld =
                localDateTime.atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
        return ld.format(formatter);
    }

    /**
     * Returns the string representation of the given <code>Date</code> based on the given ISO 639-1 language code.
     *
     * @param date LocalDateTime to format.
     * @param language ISO 639-1 (two-character) language code.
     * @should format date correctly for the given language
     * @should use English format for unknown languages
     * @return a {@link java.lang.String} object.
     */
    public static String getLocalDate(LocalDateTime date, String language) {
        if (language == null) {
            return format(date, formatterENDateTimeNoSeconds, false);
        }

        if ("de".equals(language)) {
            return format(date, formatterDEDateTimeNoSeconds, false);
        }

        return format(date, formatterENDateTimeNoSeconds, false);
    }

    /**
     * Converts the given <code>LocalDateTime</code> to a locale-based string format. This method is used by the crowdsourcing module.
     *
     * @param ldt a {@link java.time.LocalDateTime} object.
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public static String formatDate(LocalDateTime ldt, Locale locale) {
        if (ldt == null) {
            return null;
        }
        if (locale == null) {
            return format(ldt, formatterENDate, false);
        }

        switch (locale.getLanguage()) {
            case "de":
                return format(ldt, formatterDEDate, false);
            case "en":
            default:
                return format(ldt, formatterENDate, false);
        }
    }
}
