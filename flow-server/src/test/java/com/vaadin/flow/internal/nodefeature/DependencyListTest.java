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
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class DependencyListTest {
    private static final String URL = "https://example.net/";

    private MockUI ui;
    private DependencyList deps;

    @BeforeEach
    void before() {
        ui = MockUI.createUI();
        deps = ui.getInternals().getDependencyList();

        assertEquals(0, deps.getPendingSendToClient().size());
    }

    @AfterEach
    void after() {
        UI.setCurrent(null);
    }

    @Test
    void addStyleSheetDependency_eager1() {
        ui.getPage().addStyleSheet(URL);
        validateDependency(URL, Type.STYLESHEET, LoadMode.EAGER);
    }

    @Test
    void addStyleSheetDependency_eager2() {
        ui.getPage().addStyleSheet(URL, LoadMode.EAGER);
        validateDependency(URL, Type.STYLESHEET, LoadMode.EAGER);
    }

    @Test
    void addStyleSheetDependency_lazy() {
        ui.getPage().addStyleSheet(URL, LoadMode.LAZY);
        validateDependency(URL, Type.STYLESHEET, LoadMode.LAZY);
    }

    @Test
    void addStyleSheetDependency_inline() {
        ui.getPage().addStyleSheet(URL, LoadMode.INLINE);
        validateDependency(URL, Type.STYLESHEET, LoadMode.INLINE);
    }

    @Test
    void addJavaScriptDependency_eager1() {
        ui.getPage().addJavaScript(URL);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.EAGER);
    }

    @Test
    void addJavaScriptDependency_eager2() {
        ui.getPage().addJavaScript(URL, LoadMode.EAGER);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.EAGER);
    }

    @Test
    void addJavaScriptDependency_lazy() {
        ui.getPage().addJavaScript(URL, LoadMode.LAZY);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.LAZY);
    }

    @Test
    void addJavaScriptDependency_inline() {
        ui.getPage().addJavaScript(URL, LoadMode.INLINE);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.INLINE);
    }

    private void validateDependency(String url, Type dependencyType,
            LoadMode loadMode) {
        assertEquals(1, deps.getPendingSendToClient().size(),
                "Expected to receive exactly one dependency");

        Dependency dependency = deps.getPendingSendToClient().iterator().next();
        assertEquals(url, dependency.getUrl(), "URL mismatch");
        assertEquals(dependencyType, dependency.getType(), "Type mismatch");
        assertEquals(loadMode, dependency.getLoadMode(), "LoadMode mismatch");

        // Validate JSON representation includes the expected fields
        ObjectNode expectedJson = JacksonUtils.createObjectNode();
        expectedJson.put(Dependency.KEY_URL, url);
        expectedJson.put(Dependency.KEY_TYPE, dependencyType.name());
        expectedJson.put(Dependency.KEY_LOAD_MODE, loadMode.name());

        ObjectNode actualJson = JacksonUtils.getMapper()
                .valueToTree(dependency);

        // Remove the ID field from comparison since it's auto-generated for
        // some dependencies
        actualJson.remove(Dependency.KEY_ID);

        assertTrue(JacksonUtils.jsonEquals(expectedJson, actualJson),
                String.format(
                        "Dependencies' json representations are different, expected = \n'%s'\n, actual = \n'%s'",
                        expectedJson.toString(), actualJson.toString()));
    }

    @Test
    void specialUrls() {
        assertUrlUnchanged("/foo?bar");
        assertUrlUnchanged("/foo/baz?bar=http://some.thing");
        assertUrlUnchanged("/foo/baz?bar=http://some.thing&ftp://bar");
        assertUrlUnchanged("http://foo?bar");
        assertUrlUnchanged("http://foo/baz");
        assertUrlUnchanged("http://foo/baz?bar");
        assertUrlUnchanged("http://foo/baz?bar=http://some.thing");
        assertUrlUnchanged("ftp://some.host/some/where");
        assertUrlUnchanged("https://some.host/some/where");
        assertUrlUnchanged("//same.protocol.some.host/some/where");
        assertUrlUnchanged("context://foo?bar=frontend://baz");
    }

    private void assertUrlUnchanged(String url) {
        assertDependencyUrl(url, url);
    }

    private void assertDependencyUrl(String expectedUrl, String dependencyUrl) {
        addSimpleDependency(dependencyUrl);
        assertEquals(expectedUrl,
                deps.getPendingSendToClient().iterator().next().getUrl());
        deps.clearPendingSendToClient();
    }

    @Test
    void urlAddedOnlyOnce() {
        addSimpleDependency("foo/bar.js");
        addSimpleDependency("foo/bar.js");
        assertEquals(1, deps.getPendingSendToClient().size());
        deps.clearPendingSendToClient();

        addSimpleDependency("foo/bar.js");
        assertEquals(0, deps.getPendingSendToClient().size());
    }

    private void addSimpleDependency(String foo) {
        deps.add(new Dependency(Type.JAVASCRIPT, foo, LoadMode.EAGER));
    }

    @Test
    void addSameDependencyInDifferentModes_usesMostEagerLoadMode() {
        testAddingDuplicateDependencies(LoadMode.EAGER, LoadMode.EAGER,
                LoadMode.EAGER);
        testAddingDuplicateDependencies(LoadMode.EAGER, LoadMode.LAZY,
                LoadMode.EAGER);
        testAddingDuplicateDependencies(LoadMode.EAGER, LoadMode.INLINE,
                LoadMode.INLINE);

        testAddingDuplicateDependencies(LoadMode.LAZY, LoadMode.EAGER,
                LoadMode.EAGER);
        testAddingDuplicateDependencies(LoadMode.LAZY, LoadMode.LAZY,
                LoadMode.LAZY);
        testAddingDuplicateDependencies(LoadMode.LAZY, LoadMode.INLINE,
                LoadMode.INLINE);

        testAddingDuplicateDependencies(LoadMode.INLINE, LoadMode.EAGER,
                LoadMode.INLINE);
        testAddingDuplicateDependencies(LoadMode.INLINE, LoadMode.LAZY,
                LoadMode.INLINE);
        testAddingDuplicateDependencies(LoadMode.INLINE, LoadMode.INLINE,
                LoadMode.INLINE);

    }

    private void testAddingDuplicateDependencies(LoadMode first,
            LoadMode second, LoadMode expected) {

        String url = "foo/bar.js";
        Type type = Type.JAVASCRIPT;

        // need to clear so that there is no leftovers
        deps = new DependencyList();
        deps.add(new Dependency(type, url, first));
        deps.add(new Dependency(type, url, second));

        Collection<Dependency> pendingSendToClient = deps
                .getPendingSendToClient();
        assertEquals(1, pendingSendToClient.size(),
                "Expected to have only one dependency");
        assertEquals(pendingSendToClient.iterator().next().getLoadMode(),
                expected, "Wrong load mode resolved");
    }

    @Test
    void addDependencyPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            addSimpleDependency("foo" + i + "/bar.js");
        }
        long time = System.currentTimeMillis() - start;

        assertTrue(time < 500,
                "Adding 10K dependencies should take about 50ms. Took " + time
                        + "ms");
    }

    @Test
    void ensureDependenciesSentToClientHaveTheSameOrderAsAdded() {
        Dependency eagerJs = new Dependency(Type.JAVASCRIPT, "eager.js",
                LoadMode.EAGER);
        Dependency eagerCss = new Dependency(Type.STYLESHEET, "eager.css",
                LoadMode.EAGER);
        Dependency lazyJs = new Dependency(Type.JAVASCRIPT, "lazy.js",
                LoadMode.LAZY);
        Dependency lazyCss = new Dependency(Type.STYLESHEET, "lazy.css",
                LoadMode.LAZY);
        assertEquals(LoadMode.EAGER, eagerJs.getLoadMode(),
                "Expected the dependency to be eager");
        assertEquals(LoadMode.EAGER, eagerCss.getLoadMode(),
                "Expected the dependency to be eager");
        assertEquals(LoadMode.LAZY, lazyJs.getLoadMode(),
                "Expected the dependency to be lazy");
        assertEquals(LoadMode.LAZY, lazyCss.getLoadMode(),
                "Expected the dependency to be lazy");

        List<Dependency> dependencies = new ArrayList<>(
                Arrays.asList(eagerJs, eagerCss, lazyJs, lazyCss));
        assertEquals(4, dependencies.size(), "Expected to have 4 dependencies");

        Collections.shuffle(dependencies);
        dependencies.forEach(deps::add);
        List<Dependency> pendingSendToClient = new ArrayList<>(
                deps.getPendingSendToClient());

        for (int i = 0; i < pendingSendToClient.size(); i++) {
            Dependency actualDependency = pendingSendToClient.get(i);
            Dependency expectedDependency = dependencies.get(i);
            assertEquals(expectedDependency.getUrl(), actualDependency.getUrl(),
                    "Expected to have the same dependency on the same position for list, but urls do not match");
            assertEquals(expectedDependency.getLoadMode(),
                    actualDependency.getLoadMode(),
                    "Expected to have the same dependency on the same position for list, but load modes do not match");
        }
    }
}
