package com.vaadin.tests.data.converter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.vaadin.data.util.converter.StringToBooleanConverter;

import junit.framework.TestCase;

public class StringToBooleanConverterTest extends TestCase {

    StringToBooleanConverter converter = new StringToBooleanConverter();
    StringToBooleanConverter yesNoConverter = new StringToBooleanConverter(
            "yes", "no");
    StringToBooleanConverter localeConverter = new StringToBooleanConverter() {
        @Override
        public String getFalseString(Locale locale) {
            Date d = new Date(3000000000000L);
            return SimpleDateFormat
                    .getDateInstance(SimpleDateFormat.LONG, locale)
                    .format(new Date(d.getTime()
                            + (d.getTimezoneOffset() + 120) * 60 * 1000L));
        }

        @Override
        public String getTrueString(Locale locale) {
            Date d = new Date(2000000000000L);
            return SimpleDateFormat
                    .getDateInstance(SimpleDateFormat.LONG, locale)
                    .format(new Date(d.getTime()
                            + (d.getTimezoneOffset() + 120) * 60 * 1000L));
        }
    };

    public void testNullConversion() {
        assertEquals(null, converter.convertToModel(null, Boolean.class, null));
    }

    public void testEmptyStringConversion() {
        assertEquals(null, converter.convertToModel("", Boolean.class, null));
    }

    public void testValueConversion() {
        assertTrue(converter.convertToModel("true", Boolean.class, null));
        assertFalse(converter.convertToModel("false", Boolean.class, null));
    }

    public void testYesNoValueConversion() {
        assertTrue(yesNoConverter.convertToModel("yes", Boolean.class, null));
        assertFalse(yesNoConverter.convertToModel("no", Boolean.class, null));

        assertEquals("yes",
                yesNoConverter.convertToPresentation(true, String.class, null));
        assertEquals("no", yesNoConverter.convertToPresentation(false,
                String.class, null));
    }

    public void testLocale() {
        assertEquals("May 18, 2033", localeConverter.convertToPresentation(true,
                String.class, Locale.US));
        assertEquals("January 24, 2065", localeConverter
                .convertToPresentation(false, String.class, Locale.US));

        assertEquals("18. Mai 2033", localeConverter.convertToPresentation(true,
                String.class, Locale.GERMANY));
        assertEquals("24. Januar 2065", localeConverter
                .convertToPresentation(false, String.class, Locale.GERMANY));
    }

}
