package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;

public class DepsTests {

    public static final String UI_IMPORT = "@vaadin/common-frontend/ConnectionIndicator.js";

    public static <T> List<T> merge(Map<ChunkInfo, List<T>> values) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        values.forEach((key, value) -> result.addAll(value));
        return new ArrayList<>(result);
    }

    /**
     * Checks that all urls are imported in the given order and that nothing
     * else is imported.
     *
     * Ignores @vaadin/common-frontend/ConnectionIndicator.js unless included in
     * the urls list
     */
    public static void assertImports(Map<ChunkInfo, List<String>> actualUrls,
            String... expectedUrls) {
        List<String> actual = merge(actualUrls);
        Assert.assertEquals(List.of(expectedUrls), actual);
    }

    public static void assertImportsExcludingUI(
            Map<ChunkInfo, List<String>> actualUrls, String... expectedUrls) {
        for (ChunkInfo key : actualUrls.keySet()) {
            actualUrls.get(key).removeIf(imp -> imp.equals(UI_IMPORT));
        }
        assertImports(actualUrls, expectedUrls);
    }

    public static void assertImportsWithFilter(
            Map<ChunkInfo, List<String>> actualUrls, Predicate<String> filter,
            String... expectedUrls) {
        Assert.assertEquals(List.of(expectedUrls), merge(actualUrls).stream()
                .filter(filter).collect(Collectors.toList()));
    }

    /**
     * Checks that all urls are imported, does not care if something else also
     * is imported.
     */
    public static void assertHasImports(Map<ChunkInfo, List<String>> modules,
            String... urls) {
        List<String> all = merge(modules);
        Assert.assertTrue(all.containsAll(List.of(urls)));
    }

    static void assertCss(Map<ChunkInfo, List<CssData>> actual,
            List<CssData> expected) {
        Collection<CssData> all = merge(actual);
        Assert.assertEquals(expected, all);
    }

    public static <T> void assertImportCount(int expected,
            Map<ChunkInfo, List<T>> imports) {
        Assert.assertEquals(expected, merge(imports).size());
    }

}
