/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DependencyListTest {
    private static final String URL = "https://example.net/";

    private MockUI ui;
    private DependencyList deps;

    @Before
    public void before() {
        ui = MockUI.createUI();
        deps = ui.getInternals().getDependencyList();

        assertEquals(0, deps.getPendingSendToClient().size());
    }

    @After
    public void after() {
        UI.setCurrent(null);
    }

    @Test
    public void addStyleSheetDependency_eager1() {
        ui.getPage().addStyleSheet(URL);
        validateDependency(URL, Type.STYLESHEET, LoadMode.EAGER);
    }

    @Test
    public void addStyleSheetDependency_eager2() {
        ui.getPage().addStyleSheet(URL, LoadMode.EAGER);
        validateDependency(URL, Type.STYLESHEET, LoadMode.EAGER);
    }

    @Test
    public void addStyleSheetDependency_lazy() {
        ui.getPage().addStyleSheet(URL, LoadMode.LAZY);
        validateDependency(URL, Type.STYLESHEET, LoadMode.LAZY);
    }

    @Test
    public void addStyleSheetDependency_inline() {
        ui.getPage().addStyleSheet(URL, LoadMode.INLINE);
        validateDependency(URL, Type.STYLESHEET, LoadMode.INLINE);
    }

    @Test
    public void addJavaScriptDependency_eager1() {
        ui.getPage().addJavaScript(URL);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.EAGER);
    }

    @Test
    public void addJavaScriptDependency_eager2() {
        ui.getPage().addJavaScript(URL, LoadMode.EAGER);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.EAGER);
    }

    @Test
    public void addJavaScriptDependency_lazy() {
        ui.getPage().addJavaScript(URL, LoadMode.LAZY);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.LAZY);
    }

    @Test
    public void addJavaScriptDependency_inline() {
        ui.getPage().addJavaScript(URL, LoadMode.INLINE);
        validateDependency(URL, Type.JAVASCRIPT, LoadMode.INLINE);
    }

    private void validateDependency(String url, Type dependencyType,
            LoadMode loadMode) {
        assertEquals("Expected to receive exactly one dependency", 1,
                deps.getPendingSendToClient().size());

        Dependency dependency = deps.getPendingSendToClient().iterator().next();
        assertEquals("URL mismatch", url, dependency.getUrl());
        assertEquals("Type mismatch", dependencyType, dependency.getType());
        assertEquals("LoadMode mismatch", loadMode, dependency.getLoadMode());

        // Validate JSON representation includes the expected fields
        ObjectNode expectedJson = JacksonUtils.createObjectNode();
        expectedJson.put(Dependency.KEY_URL, url);
        expectedJson.put(Dependency.KEY_TYPE, dependencyType.name());
        expectedJson.put(Dependency.KEY_LOAD_MODE, loadMode.name());

        elemental.json.JsonObject actualJson = dependency.toJson();
        ObjectNode actualMapped = JacksonUtils.mapElemental(actualJson);

        // Remove the ID field from comparison since it's auto-generated for
        // some dependencies
        actualMapped.remove(Dependency.KEY_ID);

        assertTrue(String.format(
                "Dependencies' json representations are different, expected = \n'%s'\n, actual = \n'%s'",
                expectedJson.toString(), actualMapped.toString()),
                JacksonUtils.jsonEquals(expectedJson, actualMapped));
    }

    @Test
    public void specialUrls() {
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
    public void urlAddedOnlyOnce() {
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
    public void addSameDependencyInDifferentModes_usesMostEagerLoadMode() {
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
        assertEquals("Expected to have only one dependency", 1,
                pendingSendToClient.size());
        assertEquals("Wrong load mode resolved",
                pendingSendToClient.iterator().next().getLoadMode(), expected);
    }

    @Test
    public void addDependencyPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            addSimpleDependency("foo" + i + "/bar.js");
        }
        long time = System.currentTimeMillis() - start;

        assertTrue("Adding 10K dependencies should take about 50ms. Took "
                + time + "ms", time < 500);
    }

    @Test
    public void ensureDependenciesSentToClientHaveTheSameOrderAsAdded() {
        Dependency eagerJs = new Dependency(Type.JAVASCRIPT, "eager.js",
                LoadMode.EAGER);
        Dependency eagerCss = new Dependency(Type.STYLESHEET, "eager.css",
                LoadMode.EAGER);
        Dependency lazyJs = new Dependency(Type.JAVASCRIPT, "lazy.js",
                LoadMode.LAZY);
        Dependency lazyCss = new Dependency(Type.STYLESHEET, "lazy.css",
                LoadMode.LAZY);
        assertEquals("Expected the dependency to be eager", LoadMode.EAGER,
                eagerJs.getLoadMode());
        assertEquals("Expected the dependency to be eager", LoadMode.EAGER,
                eagerCss.getLoadMode());
        assertEquals("Expected the dependency to be lazy", LoadMode.LAZY,
                lazyJs.getLoadMode());
        assertEquals("Expected the dependency to be lazy", LoadMode.LAZY,
                lazyCss.getLoadMode());

        List<Dependency> dependencies = new ArrayList<>(
                Arrays.asList(eagerJs, eagerCss, lazyJs, lazyCss));
        assertEquals("Expected to have 4 dependencies", 4, dependencies.size());

        Collections.shuffle(dependencies);
        dependencies.forEach(deps::add);
        List<Dependency> pendingSendToClient = new ArrayList<>(
                deps.getPendingSendToClient());

        for (int i = 0; i < pendingSendToClient.size(); i++) {
            Dependency actualDependency = pendingSendToClient.get(i);
            Dependency expectedDependency = dependencies.get(i);
            assertEquals(
                    "Expected to have the same dependency on the same position for list, but urls do not match",
                    expectedDependency.getUrl(), actualDependency.getUrl());
            assertEquals(
                    "Expected to have the same dependency on the same position for list, but load modes do not match",
                    expectedDependency.getLoadMode(),
                    actualDependency.getLoadMode());
        }
    }
}
