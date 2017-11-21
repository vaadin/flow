package com.vaadin.shared.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import org.junit.Test;

public class SharedUtilTests {

    @Test
    public void trailingSlashIsTrimmed() {
        assertThat(SharedUtil.trimTrailingSlashes("/path/"), is("/path"));
    }

    @Test
    public void noTrailingSlashForTrimming() {
        assertThat(SharedUtil.trimTrailingSlashes("/path"), is("/path"));
    }

    @Test
    public void trailingSlashesAreTrimmed() {
        assertThat(SharedUtil.trimTrailingSlashes("/path///"), is("/path"));
    }

    @Test
    public void emptyStringIsHandled() {
        assertThat(SharedUtil.trimTrailingSlashes(""), is(""));
    }

    @Test
    public void rootSlashIsTrimmed() {
        assertThat(SharedUtil.trimTrailingSlashes("/"), is(""));
    }

    @Test
    public void camelCaseToHumanReadable() {
        assertEquals("First Name",
                SharedUtil.camelCaseToHumanFriendly("firstName"));
        assertEquals("First Name",
                SharedUtil.camelCaseToHumanFriendly("first name"));
        assertEquals("First Name2",
                SharedUtil.camelCaseToHumanFriendly("firstName2"));
        assertEquals("First",
                SharedUtil.camelCaseToHumanFriendly("first"));
        assertEquals("First",
                SharedUtil.camelCaseToHumanFriendly("First"));
        assertEquals("Some XYZ Abbreviation",
                SharedUtil.camelCaseToHumanFriendly("SomeXYZAbbreviation"));

        // Javadoc examples
        assertEquals("My Bean Container",
                SharedUtil.camelCaseToHumanFriendly("MyBeanContainer"));
        assertEquals("Awesome URL Factory",
                SharedUtil.camelCaseToHumanFriendly("AwesomeURLFactory"));
        assertEquals("Some Uri Action",
                SharedUtil.camelCaseToHumanFriendly("SomeUriAction"));

    }

    @Test
    public void splitCamelCase() {
        assertCamelCaseSplit("firstName", "first", "Name");
        assertCamelCaseSplit("fooBar", "foo", "Bar");
        assertCamelCaseSplit("fooBar", "foo", "Bar");
        assertCamelCaseSplit("fBar", "f", "Bar");
        assertCamelCaseSplit("FBar", "F", "Bar");
        assertCamelCaseSplit("MYCdi", "MY", "Cdi");
        assertCamelCaseSplit("MyCDIUI", "My", "CDIUI");
        assertCamelCaseSplit("MyCDIUITwo", "My", "CDIUI", "Two");
        assertCamelCaseSplit("first name", "first", "name");

    }

    private void assertCamelCaseSplit(String camelCaseString, String... parts) {
        String[] splitParts = SharedUtil.splitCamelCase(camelCaseString);
        assertArrayEquals(parts, splitParts);
    }

    @Test
    public void join() {
        String s1 = "foo-bar-baz";
        String s2 = "foo--bar";

        assertEquals("foobarbaz", SharedUtil.join(s1.split("-"), ""));
        assertEquals("foo!bar!baz", SharedUtil.join(s1.split("-"), "!"));
        assertEquals("foo!!bar!!baz",
                SharedUtil.join(s1.split("-"), "!!"));

        assertEquals("foo##bar", SharedUtil.join(s2.split("-"), "#"));
    }

    @Test
    public void dashSeparatedToCamelCase() {
        assertEquals(null, SharedUtil.dashSeparatedToCamelCase(null));
        assertEquals("", SharedUtil.dashSeparatedToCamelCase(""));
        assertEquals("foo", SharedUtil.dashSeparatedToCamelCase("foo"));
        assertEquals("fooBar",
                SharedUtil.dashSeparatedToCamelCase("foo-bar"));
        assertEquals("fooBar",
                SharedUtil.dashSeparatedToCamelCase("foo--bar"));
        assertEquals("fooBarBaz",
                SharedUtil.dashSeparatedToCamelCase("foo-bar-baz"));
        assertEquals("fooBarBaz",
                SharedUtil.dashSeparatedToCamelCase("foo-Bar-Baz"));
    }

    @Test
    public void camelCaseToDashSeparated() {
        assertEquals(null, SharedUtil.camelCaseToDashSeparated(null));
        assertEquals("", SharedUtil.camelCaseToDashSeparated(""));
        assertEquals("foo", SharedUtil.camelCaseToDashSeparated("foo"));
        assertEquals("foo-bar",
                SharedUtil.camelCaseToDashSeparated("fooBar"));
        assertEquals("foo--bar",
                SharedUtil.camelCaseToDashSeparated("foo--bar"));
        assertEquals("foo-bar-baz",
                SharedUtil.camelCaseToDashSeparated("fooBarBaz"));
        assertEquals("-my-bean-container",
                SharedUtil.camelCaseToDashSeparated("MyBeanContainer"));
        assertEquals("-awesome-uRL-factory",
                SharedUtil.camelCaseToDashSeparated("AwesomeURLFactory"));
        assertEquals("some-uri-action",
                SharedUtil.camelCaseToDashSeparated("someUriAction"));
    }

    @Test
    public void methodUppercaseWithTurkishLocale() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("Integer", SharedUtil.capitalize("integer"));
            assertEquals("I", SharedUtil.capitalize("i"));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    private static final String[] URIS = new String[] {
            "http://demo.vaadin.com/", //
            "https://demo.vaadin.com/", "http://demo.vaadin.com/foo",
            "http://demo.vaadin.com/foo?f", "http://demo.vaadin.com/foo?f=1",
            "http://demo.vaadin.com:1234/foo?a",
            "http://demo.vaadin.com:1234/foo#frag?fakeparam",
            // Jetspeed
            "http://localhost:8080/jetspeed/portal/_ns:Z3RlbXBsYXRlLXRvcDJfX3BhZ2UtdGVtcGxhdGVfX2RwLTFfX1AtMTJjNTRkYjdlYjUtMTAwMDJ8YzB8ZDF8aVVJREx8Zg__",
            // Liferay generated url
            "http://vaadin.com/directory?p_p_id=Directory_WAR_Directory&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_resource_id=UIDL&p_p_cacheability=cacheLevelPage&p_p_col_id=row-1&p_p_col_count=1",

    };

    @Test
    public void testParameterAdding() {
        String[] urisWithAbcdParam = new String[] {
                "http://demo.vaadin.com/?a=b&c=d",
                "https://demo.vaadin.com/?a=b&c=d",
                "http://demo.vaadin.com/foo?a=b&c=d",
                "http://demo.vaadin.com/foo?f&a=b&c=d",
                "http://demo.vaadin.com/foo?f=1&a=b&c=d",
                "http://demo.vaadin.com:1234/foo?a&a=b&c=d",
                "http://demo.vaadin.com:1234/foo?a=b&c=d#frag?fakeparam",
                "http://localhost:8080/jetspeed/portal/_ns:Z3RlbXBsYXRlLXRvcDJfX3BhZ2UtdGVtcGxhdGVfX2RwLTFfX1AtMTJjNTRkYjdlYjUtMTAwMDJ8YzB8ZDF8aVVJREx8Zg__?a=b&c=d",
                "http://vaadin.com/directory?p_p_id=Directory_WAR_Directory&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_resource_id=UIDL&p_p_cacheability=cacheLevelPage&p_p_col_id=row-1&p_p_col_count=1&a=b&c=d",

        };

        String[] urisWithAbcdParamAndFragment = new String[] {
                "http://demo.vaadin.com/?a=b&c=d#fragment",
                "https://demo.vaadin.com/?a=b&c=d#fragment",
                "http://demo.vaadin.com/foo?a=b&c=d#fragment",
                "http://demo.vaadin.com/foo?f&a=b&c=d#fragment",
                "http://demo.vaadin.com/foo?f=1&a=b&c=d#fragment",
                "http://demo.vaadin.com:1234/foo?a&a=b&c=d#fragment", "",
                "http://localhost:8080/jetspeed/portal/_ns:Z3RlbXBsYXRlLXRvcDJfX3BhZ2UtdGVtcGxhdGVfX2RwLTFfX1AtMTJjNTRkYjdlYjUtMTAwMDJ8YzB8ZDF8aVVJREx8Zg__?a=b&c=d#fragment",
                "http://vaadin.com/directory?p_p_id=Directory_WAR_Directory&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_resource_id=UIDL&p_p_cacheability=cacheLevelPage&p_p_col_id=row-1&p_p_col_count=1&a=b&c=d#fragment",

        };

        for (int i = 0; i < URIS.length; i++) {
            // Adding nothing
            assertEquals(URIS[i], SharedUtil.addGetParameters(URIS[i], ""));

            // Adding a=b&c=d
            assertEquals(urisWithAbcdParam[i],
                    SharedUtil.addGetParameters(URIS[i], "a=b&c=d"));

            // Fragments
            if (urisWithAbcdParamAndFragment[i].length() > 0) {
                assertEquals(urisWithAbcdParamAndFragment[i], SharedUtil
                        .addGetParameters(URIS[i] + "#fragment", "a=b&c=d"));

                // Empty fragment
                assertEquals(
                        urisWithAbcdParamAndFragment[i].replace("#fragment",
                                "#"),
                        SharedUtil.addGetParameters(URIS[i] + "#", "a=b&c=d"));
            }
        }
    }
}
