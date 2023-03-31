package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

public class LocalDateTimeToInstantConverterTest {

    private static final Instant INSTANT_EXAMPLE = Instant
            .parse("2007-12-03T10:15:30.00Z");
    private static final LocalDateTime LOCAL_DATE_TIME_EXAMPLE = LocalDateTime
            .of(2007, 12, 3, 10, 15, 30);
    LocalDateTimeToInstantConverter converter = new LocalDateTimeToInstantConverter(
            ZoneId.of("UTC"));

    @Test
    public void testNullConversionToModel() {
        assertEquals(converter.convertToModel(null, null), Result.ok(null));
    }

    @Test
    public void testNullConversionToPresentation() {
        assertNull(converter.convertToPresentation(null, null));
    }

    @Test
    public void testConvertToModel() {
        assertEquals(Result.ok(INSTANT_EXAMPLE),
                converter.convertToModel(LOCAL_DATE_TIME_EXAMPLE, null));
    }

    @Test
    public void testConvertToPresentation() {
        assertEquals(LOCAL_DATE_TIME_EXAMPLE,
                converter.convertToPresentation(INSTANT_EXAMPLE, null));
    }
}
