/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;

public class DepsTests {

    public static final List<String> UI_IMPORTS;

    static {
        JsModule[] annotations = UI.class.getAnnotationsByType(JsModule.class);
        UI_IMPORTS = Arrays.stream(annotations).map(JsModule::value).toList();
    }

    public static <T> List<T> merge(Map<ChunkInfo, List<T>> values) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        values.forEach((key, value) -> result.addAll(value));
        return new ArrayList<>(result);
    }

    /**
     * Checks that all urls are imported in the given order and that nothing
     * else is imported.
     */
    public static void assertImports(Map<ChunkInfo, List<String>> actualUrls,
            String... expectedUrls) {
        List<String> actual = merge(actualUrls);
        Assert.assertEquals(List.of(expectedUrls), actual);
    }

    /**
     * Checks whether a given import line originates from a UI annotation.
     * Handles both raw annotation values and resolved paths (e.g.
     * {@code ./geolocation.js} resolving to
     * {@code Frontend/generated/jar-resources/geolocation.js}).
     */
    public static boolean isUiImport(String importLine) {
        return UI_IMPORTS.stream().anyMatch(uiImport -> {
            String match = uiImport.startsWith("./") ? uiImport.substring(2)
                    : uiImport;
            return importLine.contains(match);
        });
    }

    public static void assertImportsExcludingUI(
            Map<ChunkInfo, List<String>> actualUrls, String... expectedUrls) {
        for (ChunkInfo key : actualUrls.keySet()) {
            actualUrls.get(key).removeIf(DepsTests::isUiImport);
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
