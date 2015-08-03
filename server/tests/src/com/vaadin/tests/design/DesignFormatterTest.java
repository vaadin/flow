/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.tests.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.FontIcon;
import com.vaadin.server.GenericFontIcon;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.declarative.DesignFormatter;

/**
 * Various tests related to formatter.
 * 
 * @since 7.4
 * @author Vaadin Ltd
 */
public class DesignFormatterTest {

    private DesignFormatter formatter;

    @Before
    public void setUp() {
        // initialise with default classes
        formatter = new DesignFormatter();
    }

    @Test
    public void testSupportedClasses() {

        for (Class<?> type : new Class<?>[] { boolean.class, char.class,
                byte.class, short.class, int.class, long.class, float.class,
                double.class, Boolean.class, Character.class, Byte.class,
                Short.class, Integer.class, Long.class, Float.class,
                Double.class, BigDecimal.class, String.class, Date.class,
                FileResource.class, ExternalResource.class,
                ThemeResource.class, Resource.class, TimeZone.class }) {
            assertTrue("not supported " + type.getSimpleName(),
                    formatter.canConvert(type));
        }
    }

    @Test
    public void testBoolean() {
        assertEquals("", formatter.format(true));
        assertEquals("false", formatter.format(false));

        assertEquals(true, formatter.parse("true", boolean.class));
        assertEquals(true, formatter.parse("foobar", boolean.class));
        assertEquals(true, formatter.parse("", boolean.class));
        assertEquals(false, formatter.parse("false", boolean.class));

        assertEquals(true, formatter.parse("true", Boolean.class));
        assertEquals(true, formatter.parse("foobar", Boolean.class));
        assertEquals(true, formatter.parse("", Boolean.class));
        assertEquals(false, formatter.parse("false", Boolean.class));
    }

    @Test
    public void testIntegral() {
        byte b = 123;
        assertEquals("123", formatter.format(b));
        assertEquals(b, (byte) formatter.parse("123", byte.class));
        assertEquals((Byte) b, formatter.parse("123", Byte.class));

        b = -123;
        assertEquals("-123", formatter.format(b));
        assertEquals(b, (byte) formatter.parse("-123", byte.class));
        assertEquals((Byte) b, formatter.parse("-123", Byte.class));

        short s = 12345;
        assertEquals("12345", formatter.format(s));
        assertEquals(s, (short) formatter.parse("12345", short.class));
        assertEquals((Short) s, formatter.parse("12345", Short.class));

        s = -12345;
        assertEquals("-12345", formatter.format(s));
        assertEquals(s, (short) formatter.parse("-12345", short.class));
        assertEquals((Short) s, formatter.parse("-12345", Short.class));

        int i = 123456789;
        assertEquals("123456789", formatter.format(i));
        assertEquals(i, (int) formatter.parse("123456789", int.class));
        assertEquals((Integer) i, formatter.parse("123456789", Integer.class));

        i = -123456789;
        assertEquals("-123456789", formatter.format(i));
        assertEquals(i, (int) formatter.parse("-123456789", int.class));
        assertEquals((Integer) i, formatter.parse("-123456789", Integer.class));

        long l = 123456789123456789L;
        assertEquals("123456789123456789", formatter.format(l));
        assertEquals(l,
                (long) formatter.parse("123456789123456789", long.class));
        assertEquals((Long) l,
                formatter.parse("123456789123456789", Long.class));

        l = -123456789123456789L;
        assertEquals("-123456789123456789", formatter.format(l));
        assertEquals(l,
                (long) formatter.parse("-123456789123456789", long.class));
        assertEquals((Long) l,
                formatter.parse("-123456789123456789", Long.class));
    }

    @Test
    public void testFloatingPoint() {
        float f = 123.4567f;
        assertEquals("123.457", formatter.format(f));
        assertEquals(f, formatter.parse("123.4567", float.class), 1e-4);
        assertEquals(f, formatter.parse("123.4567", Float.class), 1e-4);

        double d = 123456789.123456789;
        assertEquals("123456789.123", formatter.format(d));
        assertEquals(d, formatter.parse("123456789.123456789", double.class),
                1e-9);
        assertEquals(d, formatter.parse("123456789.123456789", Double.class),
                1e-9);

    }

    @Test
    public void testBigDecimal() {
        BigDecimal bd = new BigDecimal("123456789123456789.123456789123456789");
        assertEquals("123456789123456789.123", formatter.format(bd));
        assertEquals(bd, formatter.parse(
                "123456789123456789.123456789123456789", BigDecimal.class));
    }

    @Test
    public void testChar() {
        char c = '\uABCD';
        assertEquals("\uABCD", formatter.format(c));
        assertEquals(c, (char) formatter.parse("\uABCD", char.class));
        assertEquals((Character) c, formatter.parse("\uABCD", Character.class));

        c = 'y';
        assertEquals(c, (char) formatter.parse("yes", char.class));
    }

    @Test
    public void testString() {

        for (String s : new String[] { "", "foobar", "\uABCD", "驯鹿" }) {
            assertEquals(s, formatter.format(s));
            assertEquals(s, formatter.parse(s, String.class));
        }
    }

    @Test
    public void testDate() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2012-02-17");
        String formatted = formatter.format(date);
        Date result = formatter.parse(formatted, Date.class);

        // writing will always give full date string
        assertEquals("2012-02-17 00:00:00+0200", formatted);
        assertEquals(date, result);

        // try short date as well
        result = formatter.parse("2012-02-17", Date.class);
        assertEquals(date, result);
    }

    @Test
    public void testTimeZone() {
        TimeZone zone = TimeZone.getTimeZone("GMT+2");
        String formatted = formatter.format(zone);
        assertEquals("GMT+02:00", formatted);
        TimeZone result = formatter.parse(formatted, TimeZone.class);
        assertEquals(zone, result);
        // try shorthand notation as well
        result = formatter.parse("GMT+2", TimeZone.class);
        assertEquals(zone, result);
    }

    @Test
    public void testExternalResource() {
        String url = "://example.com/my%20icon.png?a=b";

        for (String scheme : new String[] { "http", "https", "ftp", "ftps" }) {
            Resource resource = formatter.parse(scheme + url, Resource.class);

            assertTrue(scheme + " url should be parsed as ExternalResource",
                    resource instanceof ExternalResource);
            assertEquals("parsed ExternalResource", scheme + url,
                    ((ExternalResource) resource).getURL());

            String formatted = formatter.format(new ExternalResource(scheme
                    + url));

            assertEquals("formatted ExternalResource", scheme + url, formatted);
        }
    }

    @Test
    public void testResourceFormat() {
        String httpUrl = "http://example.com/icon.png";
        String httpsUrl = "https://example.com/icon.png";
        String themePath = "icons/icon.png";
        String fontAwesomeUrl = "fonticon://FontAwesome/f0f9";
        String someOtherFontUrl = "fonticon://SomeOther/F0F9";
        String fileSystemPath = "c:\\app\\resources\\icon.png";

        assertEquals(httpUrl, formatter.format(new ExternalResource(httpUrl)));
        assertEquals(httpsUrl, formatter.format(new ExternalResource(httpsUrl)));
        assertEquals(ApplicationConstants.THEME_PROTOCOL_PREFIX + themePath,
                formatter.format(new ThemeResource(themePath)));

        assertEquals(fontAwesomeUrl, formatter.format(FontAwesome.AMBULANCE));
        assertEquals(someOtherFontUrl.toLowerCase(),
                formatter.format(new GenericFontIcon("SomeOther", 0xf0f9))
                        .toLowerCase());

        assertEquals(fileSystemPath,
                formatter.format(new FileResource(new File(fileSystemPath))));
    }

    @Test(expected = ConversionException.class)
    public void testResourceParseException() {
        String someRandomResourceUrl = "random://url";
        formatter.parse(someRandomResourceUrl, Resource.class);
    }

    @Test(expected = ConversionException.class)
    public void testResourceFormatException() {
        formatter.format(new Resource() { // must use unknown resource type
                    @Override
                    public String getMIMEType() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                });
    }

    @Test
    public void testResourceParse() {
        String httpUrl = "http://example.com/icon.png";
        String httpsUrl = "https://example.com/icon.png";
        String themePath = "icons/icon.png";
        String fontAwesomeUrl = "fonticon://FontAwesome/f0f9";
        String someOtherFont = "fonticon://SomeOther/F0F9";
        String fontAwesomeUrlOld = "font://AMBULANCE";
        String fileSystemPath = "c:\\app\\resources\\icon.png";

        assertEquals(new ExternalResource(httpUrl).getURL(),
                formatter.parse(httpUrl, ExternalResource.class).getURL());
        assertEquals(new ExternalResource(httpsUrl).getURL(),
                formatter.parse(httpsUrl, ExternalResource.class).getURL());
        assertEquals(
                new ThemeResource(themePath),
                formatter.parse(ApplicationConstants.THEME_PROTOCOL_PREFIX
                        + themePath, ThemeResource.class));
        assertEquals(FontAwesome.AMBULANCE,
                formatter.parse(fontAwesomeUrlOld, FontAwesome.class));
        assertEquals(FontAwesome.AMBULANCE,
                formatter.parse(fontAwesomeUrl, FontAwesome.class));
        assertEquals(new GenericFontIcon("SomeOther", 0xF0F9),
                formatter.parse(someOtherFont, FontIcon.class));

        assertEquals(
                new FileResource(new File(fileSystemPath)).getSourceFile(),
                formatter.parse(fileSystemPath, FileResource.class)
                        .getSourceFile());

    }

}
