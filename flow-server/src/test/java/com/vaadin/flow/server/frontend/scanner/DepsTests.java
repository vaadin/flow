package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

public class DepsTests {

    public static <T> List<T> merge(Map<String, List<T>> css) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        css.forEach((key, value) -> result.addAll(value));
        return new ArrayList<>(result);
    }

    /**
     * Checks that all urls are imported in the given order and that nothing
     * else is imported.
     *
     * Ignores @vaadin/common-frontend/ConnectionIndicator.js unless included in
     * the urls list
     */
    public static void assertImports(Map<String, List<String>> modules,
            String... urls) {
        // Ignore the common UI import
        // @vaadin/common-frontend/ConnectionIndicator.js to makes test simpler
        // unless it is specifically requested
        List<String> all = merge(modules);
        if (!Arrays.asList(urls)
                .contains("@vaadin/common-frontend/ConnectionIndicator.js")) {
            all.remove("@vaadin/common-frontend/ConnectionIndicator.js");
        }
        Assert.assertEquals(List.of(urls), all);
    }

    /**
     * Checks that all urls are imported, does not care if something else also
     * is imported.
     */
    public static void assertHasImports(Map<String, List<String>> modules,
            String... urls) {
        List<String> all = merge(modules);
        Assert.assertTrue(all.containsAll(List.of(urls)));
    }

    static void assertCss(Map<String, List<CssData>> css,
            List<CssData> expected) {
        Collection<CssData> all = merge(css);
        Assert.assertEquals(expected, all);
    }

    public static <T> void assertImportCount(int expected,
            Map<String, List<T>> imports) {
        Assert.assertEquals(expected, merge(imports).size());
    }

}
