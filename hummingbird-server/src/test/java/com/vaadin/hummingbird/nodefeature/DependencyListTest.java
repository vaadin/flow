/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.nodefeature;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Dependency;
import com.vaadin.ui.Dependency.Type;
import com.vaadin.ui.DependencyList;

import elemental.json.Json;
import elemental.json.JsonObject;

public class DependencyListTest {
    private MockUI ui = new MockUI();
    private DependencyList deps = ui.getInternals().getDependencyList();

    @Test
    public void addAbsoluteStyleSheetDependency() {
        Assert.assertEquals(0, deps.getPendingSendToClient().length());

        ui.getPage().addStyleSheet("/styleSheetUrl");

        JsonObject expectedStyleSheetJson = Json.createObject();
        expectedStyleSheetJson.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_STYLESHEET);
        expectedStyleSheetJson.put(DependencyList.KEY_URL, "/styleSheetUrl");

        Assert.assertEquals(1, deps.getPendingSendToClient().length());
        Assert.assertTrue(JsonUtil.jsonEquals(expectedStyleSheetJson,
                deps.getPendingSendToClient().get(0)));
    }

    @Test
    public void addRelativeStyleSheetDependency() {
        Assert.assertEquals(0, deps.getPendingSendToClient().length());

        ui.getPage().addStyleSheet("styleSheetUrl");

        JsonObject expectedStyleSheetJson = Json.createObject();
        expectedStyleSheetJson.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_STYLESHEET);
        expectedStyleSheetJson.put(DependencyList.KEY_URL, "styleSheetUrl");

        Assert.assertEquals(1, deps.getPendingSendToClient().length());
        Assert.assertTrue(JsonUtil.jsonEquals(expectedStyleSheetJson,
                deps.getPendingSendToClient().get(0)));
    }

    @Test
    public void addAbsoluteJavaScriptDependency() {
        Assert.assertEquals(0, deps.getPendingSendToClient().length());
        ui.getPage().addJavaScript("/jsUrl");

        JsonObject expectedJsJson = Json.createObject();
        expectedJsJson.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_JAVASCRIPT);
        expectedJsJson.put(DependencyList.KEY_URL, "/jsUrl");

        Assert.assertEquals(1, deps.getPendingSendToClient().length());
        Assert.assertTrue(JsonUtil.jsonEquals(expectedJsJson,
                deps.getPendingSendToClient().get(0)));

    }

    @Test
    public void addRelativeJavaScriptDependency() {
        Assert.assertEquals(0, deps.getPendingSendToClient().length());
        ui.getPage().addJavaScript("jsUrl");

        JsonObject expectedJsJson = Json.createObject();
        expectedJsJson.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_JAVASCRIPT);
        expectedJsJson.put(DependencyList.KEY_URL, "jsUrl");

        Assert.assertEquals(1, deps.getPendingSendToClient().length());
        Assert.assertTrue(JsonUtil.jsonEquals(expectedJsJson,
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
        assertUrlPrefixed("foo?bar");
        assertUrlPrefixed("foo?bar=http://yah");
        assertUrlPrefixed("foo/baz?bar=http://some.thing");
        assertUrlPrefixed("foo/baz?bar=http://some.thing&ftp://bar");
    }

    private void assertUrlUnchanged(String url) {
        deps.add(new Dependency(Type.JAVASCRIPT, url));
        Assert.assertEquals(url,
                ((JsonObject) deps.getPendingSendToClient().get(0))
                        .getString(DependencyList.KEY_URL));
        deps.clearPendingSendToClient();
    }

    private void assertUrlPrefixed(String url) {
        deps.add(new Dependency(Type.JAVASCRIPT, url));
        Assert.assertEquals(url,
                ((JsonObject) deps.getPendingSendToClient().get(0))
                        .getString(DependencyList.KEY_URL));
        deps.clearPendingSendToClient();
    }

    @Test
    public void urlAddedOnlyOnce() {
        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js"));
        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js"));
        Assert.assertEquals(1, deps.getPendingSendToClient().length());
        deps.clearPendingSendToClient();

        deps.add(new Dependency(Type.JAVASCRIPT, "foo/bar.js"));
        Assert.assertEquals(0, deps.getPendingSendToClient().length());
    }

    @Test
    public void addDependencyPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            deps.add(new Dependency(Type.JAVASCRIPT, "foo" + i + "/bar.js"));
        }
        long time = System.currentTimeMillis() - start;

        Assert.assertTrue(
                "Adding 10K dependencies should take about 50ms. Took " + time
                        + "ms",
                time < 500);
    }
}
