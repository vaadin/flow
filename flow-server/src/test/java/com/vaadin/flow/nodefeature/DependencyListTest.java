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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.util.JsonUtils;
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
    public void addStyleSheetDependency_blocking1() {
        ui.getPage().addStyleSheet(URL);
        validateDependency(URL, DependencyList.TYPE_STYLESHEET, true);
    }

    @Test
    public void addStyleSheetDependency_blocking2() {
        ui.getPage().addStyleSheet(URL, true);
        validateDependency(URL, DependencyList.TYPE_STYLESHEET, true);
    }

    @Test
    public void addStyleSheetDependency_nonBlocking() {
        ui.getPage().addStyleSheet(URL, false);
        validateDependency(URL, DependencyList.TYPE_STYLESHEET, false);
    }

    @Test
    public void addJavaScriptDependency_blocking1() {
        ui.getPage().addJavaScript(URL);
        validateDependency(URL, DependencyList.TYPE_JAVASCRIPT, true);
    }

    @Test
    public void addJavaScriptDependency_blocking2() {
        ui.getPage().addJavaScript(URL, true);
        validateDependency(URL, DependencyList.TYPE_JAVASCRIPT, true);
    }

    @Test
    public void addJavaScriptDependency_nonBlocking() {
        ui.getPage().addJavaScript(URL, false);
        validateDependency(URL, DependencyList.TYPE_JAVASCRIPT, false);
    }

    @Test
    public void addHtmlDependency_blocking1() {
        ui.getPage().addHtmlImport(URL);
        validateDependency(URL, DependencyList.TYPE_HTML_IMPORT, true);
    }

    @Test
    public void addHtmlDependency_blocking2() {
        ui.getPage().addHtmlImport(URL, true);
        validateDependency(URL, DependencyList.TYPE_HTML_IMPORT, true);
    }

    @Test
    public void addHtmlDependency_nonBlocking() {
        ui.getPage().addHtmlImport(URL, false);
        validateDependency(URL, DependencyList.TYPE_HTML_IMPORT, false);
    }

    private void validateDependency(String url, String dependencyType,
            boolean blocking) {
        JsonObject expectedJson = Json.createObject();
        expectedJson.put(DependencyList.KEY_URL, url);
        expectedJson.put(DependencyList.KEY_TYPE, dependencyType);
        expectedJson.put(DependencyList.KEY_BLOCKING, blocking);

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
        deps.add(new Dependency(Type.JAVASCRIPT, url, true));
        assertEquals(url, ((JsonObject) deps.getPendingSendToClient().get(0))
                .getString(DependencyList.KEY_URL));
        deps.clearPendingSendToClient();
    }

    @Test
    public void urlAddedOnlyOnce() {
        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js", true));
        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js", true));
        assertEquals(1, deps.getPendingSendToClient().length());
        deps.clearPendingSendToClient();

        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js", true));
        assertEquals(0, deps.getPendingSendToClient().length());
    }

    @Test
    public void addSameDependencyInDifferentModes() {
        String url = "foo/bar.js";
        Type type = Type.JAVASCRIPT;

        deps.add(new Dependency(type, url, true));
        deps.add(new Dependency(type, url, false));

        assertEquals(1, deps.getPendingSendToClient().length());

        JsonObject dependency = deps.getPendingSendToClient().getObject(0);
        assertEquals("Dependency should be added with url specified", url,
                dependency.getString(DependencyList.KEY_URL));
        assertEquals("Dependency should be added with its key",
                DependencyList.TYPE_JAVASCRIPT,
                dependency.getString(DependencyList.KEY_TYPE));
        assertTrue(
                "Dependency in different modes should be added as blocking one",
                dependency.getBoolean(DependencyList.KEY_BLOCKING));
    }

    @Test
    public void addDependencyPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            deps.add(new Dependency(Type.JAVASCRIPT, "foo" + i + "/bar.js",
                    true));
        }
        long time = System.currentTimeMillis() - start;

        assertTrue("Adding 10K dependencies should take about 50ms. Took "
                + time + "ms", time < 500);
    }

    @Test
    public void ensureDependenciesSentToClientHaveTheSameOrderAsAdded() {
        Dependency html_dep_blocking = new Dependency(Type.HTML_IMPORT,
                "blocking.html", true);
        Dependency js_dep_blocking = new Dependency(Type.JAVASCRIPT,
                "blocking.js", true);
        Dependency css_dep_blocking = new Dependency(Type.STYLESHEET,
                "blocking.css", true);
        Dependency html_dep_non_blocking = new Dependency(Type.HTML_IMPORT,
                "non_blocking.html", false);
        Dependency js_dep_non_blocking = new Dependency(Type.JAVASCRIPT,
                "non_blocking.js", false);
        Dependency css_dep_non_blocking = new Dependency(Type.STYLESHEET,
                "non_blocking.css", false);
        assertTrue("Expected the dependency to be blocking",
                html_dep_blocking.isBlocking());
        assertTrue("Expected the dependency to be blocking",
                js_dep_blocking.isBlocking());
        assertTrue("Expected the dependency to be blocking",
                css_dep_blocking.isBlocking());
        assertFalse("Expected the dependency to be non-blocking",
                html_dep_non_blocking.isBlocking());
        assertFalse("Expected the dependency to be non-blocking",
                js_dep_non_blocking.isBlocking());
        assertFalse("Expected the dependency to be non-blocking",
                css_dep_non_blocking.isBlocking());

        List<Dependency> dependencies = new ArrayList<>(
                Arrays.asList(html_dep_blocking, js_dep_blocking,
                        css_dep_blocking, html_dep_non_blocking,
                        js_dep_non_blocking, css_dep_non_blocking));
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
                    "Expected to have the same dependency on the same position for list, but blocking parameter values do not match",
                    expectedDependency.isBlocking(),
                    actualDependency.getBoolean(DependencyList.KEY_BLOCKING));
        }
    }
}
