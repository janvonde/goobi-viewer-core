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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class DateToolsTest extends AbstractTest {

    /**
     * @see DateTools#getLocalDate(Date,String)
     * @verifies format date correctly for the given language
     */
    @Test
    public void getLocalDate_shouldFormatDateCorrectlyForTheGivenLanguage() throws Exception {
        LocalDateTime date = LocalDateTime.now()
                .withYear(1980)
                .withMonth(7)
                .withDayOfMonth(10)
                .withHour(13)
                .withMinute(15)
                .withSecond(30);
        Assert.assertEquals("10.07.1980 13:15", DateTools.getLocalDate(date, "de"));
        Assert.assertEquals("07/10/1980 1:15 PM", DateTools.getLocalDate(date, "en"));
    }

    /**
     * @see DateTools#getLocalDate(Date,String)
     * @verifies use English format for unknown languages
     */
    @Test
    public void getLocalDate_shouldUseEnglishFormatForUnknownLanguages() throws Exception {
        LocalDateTime date = LocalDateTime.now()
                .withYear(1980)
                .withMonth(7)
                .withDayOfMonth(10)
                .withHour(13)
                .withMinute(15)
                .withSecond(30);
        Assert.assertEquals("07/10/1980 1:15 PM", DateTools.getLocalDate(date, "eu"));
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse iso date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseIsoDateFormatsCorrectly() throws Exception {
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("2017-12-19 00:00:00", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("2017-12-19 00:00:00.000", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("2017-12-19T00:00:00", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("2017-12-19T00:00:00Z", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("2017-12-19", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("2017-12", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(1, date.getDayOfMonth());
        }
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse german date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseGermanDateFormatsCorrectly() throws Exception {
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("19.12.2017", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("20.12.2017 01:02:03", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(20, date.getDayOfMonth());
            Assert.assertEquals(1, date.getHour());
            Assert.assertEquals(2, date.getMinute());
            Assert.assertEquals(3, date.getSecond());
        }
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse english date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseEnglishDateFormatsCorrectly() throws Exception {
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("12/20/2017", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(20, date.getDayOfMonth());
        }
        {
            LocalDateTime date = DateTools.parseDateTimeFromString("12/19/2017 1:02:03 AM", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthValue());
            Assert.assertEquals(19, date.getDayOfMonth());
            Assert.assertEquals(1, date.getHour());
            Assert.assertEquals(2, date.getMinute());
            Assert.assertEquals(3, date.getSecond());
        }
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse chinese date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseChineseDateFormatsCorrectly() throws Exception {
        LocalDateTime date = DateTools.parseDateTimeFromString("2017.12.19", false);
        Assert.assertNotNull(date);
        Assert.assertEquals(2017, date.getYear());
        Assert.assertEquals(12, date.getMonthValue());
        Assert.assertEquals(19, date.getDayOfMonth());
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse japanese date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseJapaneseDateFormatsCorrectly() throws Exception {
        LocalDateTime date = DateTools.parseDateTimeFromString("2017/12/19", false);
        Assert.assertNotNull(date);
        Assert.assertEquals(2017, date.getYear());
        Assert.assertEquals(12, date.getMonthValue());
        Assert.assertEquals(19, date.getDayOfMonth());
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse iso date as UTC correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseIsoDateAsUTCCorrectly() throws Exception {
        LocalDateTime date = DateTools.parseDateTimeFromString("2017-12-19T01:01:00+01:00", true, 1);
        Assert.assertNotNull(date);
        Assert.assertEquals(2017, date.getYear());
        Assert.assertEquals(12, date.getMonthValue());
        Assert.assertEquals(19, date.getDayOfMonth());
        Assert.assertEquals(0, date.getHour());
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies return null if unsupported format
     */
    @Test
    public void parseDateTimeFromString_shouldReturnNullIfUnsupportedFormat() throws Exception {
        Assert.assertNull(DateTools.parseDateTimeFromString("2017_12_20", false));
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies throw IllegalArgumentException if dateString is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseDateTimeFromString_shouldThrowIllegalArgumentExceptionIfDateStringIsNull() throws Exception {
        DateTools.parseDateTimeFromString(null, false);
    }

    /**
     * @see DateTools#parseMultipleDatesFromString(String)
     * @verifies parse single date correctly
     */
    @Test
    public void parseMultipleDatesFromString_shouldParseSingleDateCorrectly() throws Exception {
        List<LocalDateTime> result = DateTools.parseMultipleDatesFromString("2018-11-20");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2018-11-20", DateTools.format(result.get(0), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see DateTools#parseMultipleDatesFromString(String)
     * @verifies parse multiple dates correctly
     */
    @Test
    public void parseMultipleDatesFromString_shouldParseMultipleDatesCorrectly() throws Exception {
        List<LocalDateTime> result = DateTools.parseMultipleDatesFromString("2018-11-19 / 2018-11-20");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("2018-11-19", DateTools.format(result.get(0), DateTools.formatterISO8601Date, false));
        Assert.assertEquals("2018-11-20", DateTools.format(result.get(1), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see DateTools#parseMultipleDatesFromString(String)
     * @verifies parse dates in parentheses correctly
     */
    @Test
    public void parseMultipleDatesFromString_shouldParseDatesInParenthesesCorrectly() throws Exception {
        List<LocalDateTime> result = DateTools.parseMultipleDatesFromString("(2018-11-19) / (2018-11-20)");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("2018-11-19", DateTools.format(result.get(0), DateTools.formatterISO8601Date, false));
        Assert.assertEquals("2018-11-20", DateTools.format(result.get(1), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see DateTools#getLocalDateTimeFromMillis(long,boolean)
     * @verifies create LocalDateTime correctly
     */
    @Test
    public void getLocalDateTimeFromMillis_shouldCreateLocalDateTimeCorrectly() throws Exception {
        LocalDateTime ldt = DateTools.getLocalDateTimeFromMillis(1603905300000L, true);
        Assert.assertNotNull(ldt);
        Assert.assertEquals(2020, ldt.getYear());
        Assert.assertEquals(10, ldt.getMonthValue());
        Assert.assertEquals(28, ldt.getDayOfMonth());
        Assert.assertEquals(17, ldt.getHour());
        Assert.assertEquals(15, ldt.getMinute());
        Assert.assertEquals(0, ldt.getSecond());
    }
}
