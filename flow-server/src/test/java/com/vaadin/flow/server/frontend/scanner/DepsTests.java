package com.vaadin.flow.server.frontend.scanner;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;

public class DepsTests {

    public static final String UI_IMPORT = "@vaadin/common-frontend/ConnectionIndicator.js";

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

    public static void assertImportsExcludingUI(
            List<String> actualUrls, String... expectedUrls) {
        actualUrls.removeIf(imp -> imp.equals(UI_IMPORT));
        assertImports(actualUrls, expectedUrls);
    }

    public static void assertImportsWithFilter(
            List<String> actualUrls, Predicate<String> filter,
            String... expectedUrls) {
        Assert.assertEquals(List.of(expectedUrls), actualUrls.stream()
                .filter(filter).collect(Collectors.toList()));
    }

    public static <T> void assertImportCount(int expected,
            List<T> imports) {
        Assert.assertEquals(expected, imports.size());
    }

}
