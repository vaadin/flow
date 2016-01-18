package com.vaadin.tests.data.converter;

import java.util.Date;
import java.util.Locale;

import com.vaadin.data.util.converter.StringToDateConverter;

import junit.framework.TestCase;

public class StringToDateConverterTest extends TestCase {

    StringToDateConverter converter = new StringToDateConverter();

    public void testNullConversion() {
        assertEquals(null, converter.convertToModel(null, Date.class, null));
    }

    public void testEmptyStringConversion() {
        assertEquals(null, converter.convertToModel("", Date.class, null));
    }

    public void testValueConversion() {
        assertEquals(new Date(100, 0, 1), converter.convertToModel(
                "Jan 1, 2000 12:00:00 AM", Date.class, Locale.ENGLISH));
    }
}
