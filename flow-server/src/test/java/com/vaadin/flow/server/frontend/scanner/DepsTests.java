package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;

public class DepsTests {

    private static final Object UI_IMPORT = "@vaadin/common-frontend/ConnectionIndicator.js";

    /**
     * Checks that all urls are imported in the given order and that nothing
     * else is imported.
     *
     * Ignores @vaadin/common-frontend/ConnectionIndicator.js unless included in
     * the urls list
     */
    public static void assertImports(List<String> actualUrls,
            String... expectedUrls) {
        Assert.assertEquals(List.of(expectedUrls), actualUrls);
    }

    public static void assertImportsExcludingUI(List<String> actualUrls,
            String... expectedUrls) {
        ArrayList<String> actual = new ArrayList<>(actualUrls);
        actual.remove(UI_IMPORT);
        assertImports(actual, expectedUrls);
    }

    /**
     * Checks that all urls are imported, does not care if something else also
     * is imported.
     */
    public static void assertHasImports(List<String> modules, String... urls) {
        Assert.assertTrue(modules.containsAll(List.of(urls)));
    }

    static void assertCss(List<CssData> actual, List<CssData> expected) {
        Assert.assertEquals(expected, actual);
    }

    public static <T> void assertImportCount(int expected,
            Collection<T> imports) {
        Assert.assertEquals(expected, imports.size());
    }

}
