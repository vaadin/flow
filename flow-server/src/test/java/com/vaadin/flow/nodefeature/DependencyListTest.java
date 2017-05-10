/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.util.JsonUtils;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Dependency;
import com.vaadin.ui.Dependency.Type;
import com.vaadin.ui.DependencyList;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class DependencyListTest {
    private static final String URL = "https://example.net/";

    private MockUI ui;
    private DependencyList deps;

    @Before
    public void before() {
        ui = new MockUI();
        deps = ui.getInternals().getDependencyList();

        assertEquals(0, deps.getPendingSendToClient().length());
    }

    @Test
    public void addStyleSheetDependency_eager1() {
        ui.getPage().addStyleSheet(URL);
        validateDependency(URL, DependencyList.TYPE_STYLESHEET, LoadMode.EAGER);
    }

    @Test
    public void addStyleSheetDependency_eager2() {
        ui.getPage().addStyleSheet(URL, LoadMode.EAGER);
        validateDependency(URL, DependencyList.TYPE_STYLESHEET, LoadMode.EAGER);
    }

    @Test
    public void addStyleSheetDependency_lazy() {
        ui.getPage().addStyleSheet(URL, LoadMode.LAZY);
        validateDependency(URL, DependencyList.TYPE_STYLESHEET, LoadMode.LAZY);
    }

    @Test
    public void addJavaScriptDependency_eager1() {
        ui.getPage().addJavaScript(URL);
        validateDependency(URL, DependencyList.TYPE_JAVASCRIPT, LoadMode.EAGER);
    }

    @Test
    public void addJavaScriptDependency_eager2() {
        ui.getPage().addJavaScript(URL, LoadMode.EAGER);
        validateDependency(URL, DependencyList.TYPE_JAVASCRIPT, LoadMode.EAGER);
    }

    @Test
    public void addJavaScriptDependency_lazy() {
        ui.getPage().addJavaScript(URL, LoadMode.LAZY);
        validateDependency(URL, DependencyList.TYPE_JAVASCRIPT, LoadMode.LAZY);
    }

    @Test
    public void addHtmlDependency_eager1() {
        ui.getPage().addHtmlImport(URL);
        validateDependency(URL, DependencyList.TYPE_HTML_IMPORT, LoadMode.EAGER);
    }

    @Test
    public void addHtmlDependency_eager2() {
        ui.getPage().addHtmlImport(URL, LoadMode.EAGER);
        validateDependency(URL, DependencyList.TYPE_HTML_IMPORT, LoadMode.EAGER);
    }

    @Test
    public void addHtmlDependency_lazy() {
        ui.getPage().addHtmlImport(URL, LoadMode.LAZY);
        validateDependency(URL, DependencyList.TYPE_HTML_IMPORT, LoadMode.LAZY);
    }

    private void validateDependency(String url, String dependencyType,
            LoadMode loadMode) {
        JsonObject expectedJson = Json.createObject();
        expectedJson.put(DependencyList.KEY_URL, url);
        expectedJson.put(DependencyList.KEY_TYPE, dependencyType);
        expectedJson.put(DependencyList.KEY_LOAD_MODE, loadMode.name());

        assertEquals("Expected to receive exactly one dependency", 1,
                deps.getPendingSendToClient().length());
        assertTrue(
                String.format(
                        "Dependencies' json representations are different, expected = \n'%s'\n, actual = \n'%s'",
                        expectedJson.toJson(),
                        deps.getPendingSendToClient().get(0).toJson()),
                JsonUtils.jsonEquals(expectedJson,
                        deps.getPendingSendToClient().get(0)));
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
        assertUrlUnchanged("foo?bar");
        assertUrlUnchanged("foo?bar=http://yah");
        assertUrlUnchanged("foo/baz?bar=http://some.thing");
        assertUrlUnchanged("foo/baz?bar=http://some.thing&ftp://bar");
    }

    private void assertUrlUnchanged(String url) {
        deps.add(new Dependency(Type.JAVASCRIPT, url, LoadMode.EAGER));
        assertEquals(url, ((JsonObject) deps.getPendingSendToClient().get(0))
                .getString(DependencyList.KEY_URL));
        deps.clearPendingSendToClient();
    }

    @Test
    public void urlAddedOnlyOnce() {
        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js", LoadMode.EAGER));
        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js", LoadMode.EAGER));
        assertEquals(1, deps.getPendingSendToClient().length());
        deps.clearPendingSendToClient();

        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js", LoadMode.EAGER));
        assertEquals(0, deps.getPendingSendToClient().length());
    }

    @Test
    public void addSameDependencyInDifferentModes() {
        String url = "foo/bar.js";
        Type type = Type.JAVASCRIPT;

        deps.add(new Dependency(type, url, LoadMode.EAGER));
        deps.add(new Dependency(type, url, LoadMode.LAZY));

        assertEquals(1, deps.getPendingSendToClient().length());

        JsonObject dependency = deps.getPendingSendToClient().getObject(0);
        assertEquals("Dependency should be added with url specified", url,
                dependency.getString(DependencyList.KEY_URL));
        assertEquals("Dependency should be added with its key",
                DependencyList.TYPE_JAVASCRIPT,
                dependency.getString(DependencyList.KEY_TYPE));
        assertEquals(
                String.format("Dependency in different modes should be added with mode = %s", LoadMode.EAGER),
                LoadMode.EAGER.name(),
                dependency.getString(DependencyList.KEY_LOAD_MODE));
    }

    @Test
    public void addDependencyPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            deps.add(new Dependency(Type.JAVASCRIPT, "foo" + i + "/bar.js",
                    LoadMode.EAGER));
        }
        long time = System.currentTimeMillis() - start;

        assertTrue("Adding 10K dependencies should take about 50ms. Took "
                + time + "ms", time < 500);
    }

    @Test
    public void ensureDependenciesSentToClientHaveTheSameOrderAsAdded() {
        Dependency eagerHtml = new Dependency(Type.HTML_IMPORT,
                "eager.html", LoadMode.EAGER);
        Dependency eagerJs = new Dependency(Type.JAVASCRIPT,
                "eager.js", LoadMode.EAGER);
        Dependency eagerCss = new Dependency(Type.STYLESHEET,
                "eager.css", LoadMode.EAGER);
        Dependency lazyHtml = new Dependency(Type.HTML_IMPORT,
                "lazy.html", LoadMode.LAZY);
        Dependency lazyJs = new Dependency(Type.JAVASCRIPT,
                "lazy.js", LoadMode.LAZY);
        Dependency lazyCss = new Dependency(Type.STYLESHEET,
                "lazy.css", LoadMode.LAZY);
        assertEquals("Expected the dependency to be eager",
                LoadMode.EAGER, eagerHtml.getLoadMode());
        assertEquals("Expected the dependency to be eager",
                LoadMode.EAGER, eagerJs.getLoadMode());
        assertEquals("Expected the dependency to be eager",
                LoadMode.EAGER, eagerCss.getLoadMode());
        assertEquals("Expected the dependency to be lazy",
                LoadMode.LAZY, lazyHtml.getLoadMode());
        assertEquals("Expected the dependency to be lazy",
                LoadMode.LAZY, lazyJs.getLoadMode());
        assertEquals("Expected the dependency to be lazy",
                LoadMode.LAZY, lazyCss.getLoadMode());

        List<Dependency> dependencies = new ArrayList<>(
                Arrays.asList(eagerHtml, eagerJs,
                        eagerCss, lazyHtml,
                        lazyJs, lazyCss));
        assertEquals("Expected to have 6 dependencies", 6, dependencies.size());

        Collections.shuffle(dependencies);
        dependencies.forEach(deps::add);
        JsonArray pendingSendToClient = deps.getPendingSendToClient();

        for (int i = 0; i < pendingSendToClient.length(); i++) {
            JsonObject actualDependency = pendingSendToClient.getObject(i);
            Dependency expectedDependency = dependencies.get(i);
            assertEquals(
                    "Expected to have the same dependency on the same position for list, but urls do not match",
                    expectedDependency.getUrl(),
                    actualDependency.getString(DependencyList.KEY_URL));
            assertEquals(
                    "Expected to have the same dependency on the same position for list, but load modes do not match",
                    expectedDependency.getLoadMode().name(),
                    actualDependency.getString(DependencyList.KEY_LOAD_MODE));
        }
    }
}
