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
package com.vaadin.client;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.ui.DependencyList;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class DependencyLoaderTest {

    public static class MockResourceLoader extends ResourceLoader {
        protected MockResourceLoader() {
            super(false);
        }

        List<String> loadingStyles = new ArrayList<>();
        List<String> loadingScripts = new ArrayList<>();
        List<String> loadingHtml = new ArrayList<>();

        @Override
        public void loadHtml(String htmlUrl,
                ResourceLoadListener resourceLoadListener) {
            loadingHtml.add(htmlUrl);
        };

        @Override
        public void loadScript(String scriptUrl,
                ResourceLoadListener resourceLoadListener) {
            loadingScripts.add(scriptUrl);
        };

        @Override
        public void loadStylesheet(String stylesheetUrl,
                ResourceLoadListener resourceLoadListener) {
            loadingStyles.add(stylesheetUrl);
        };
    }

    private Registry registry = new Registry() {
        {
            set(ResourceLoader.class, new MockResourceLoader());
            set(URIResolver.class, new URIResolver(this));
        }
    };
    StateTree tree = new StateTree(registry);
    StateNode rootNode = tree.getRootNode();

    @Test
    public void loadStylesheet() {
        String TEST_URL = "http://foo.bar/baz";
        DependencyLoader loader = new DependencyLoader(registry);

        JsonObject styleDep = Json.createObject();
        styleDep.put(DependencyList.KEY_TYPE, DependencyList.TYPE_STYLESHEET);
        styleDep.put(DependencyList.KEY_URL, TEST_URL);

        JsonArray deps = Json.createArray();
        deps.set(0, styleDep);
        loader.loadDependencies(deps);

        Assert.assertArrayEquals(new String[] { TEST_URL },
                ((MockResourceLoader) registry
                        .getResourceLoader()).loadingStyles.toArray());
    }

    @Test
    public void loadScript() {
        String TEST_URL = "http://foo.bar/baz.js";

        DependencyLoader loader = new DependencyLoader(registry);

        JsonObject styleDep = Json.createObject();
        styleDep.put(DependencyList.KEY_TYPE, DependencyList.TYPE_JAVASCRIPT);
        styleDep.put(DependencyList.KEY_URL, TEST_URL);

        JsonArray deps = Json.createArray();
        deps.set(0, styleDep);
        loader.loadDependencies(deps);

        Assert.assertArrayEquals(new String[] { TEST_URL },
                ((MockResourceLoader) registry
                        .getResourceLoader()).loadingScripts.toArray());
    }

    @Test
    public void loadHtml() {
        String TEST_URL = "http://foo.bar/baz.html";

        DependencyLoader loader = new DependencyLoader(registry);

        JsonObject styleDep = Json.createObject();
        styleDep.put(DependencyList.KEY_TYPE, DependencyList.TYPE_HTML_IMPORT);
        styleDep.put(DependencyList.KEY_URL, TEST_URL);

        JsonArray deps = Json.createArray();
        deps.set(0, styleDep);
        loader.loadDependencies(deps);

        Assert.assertArrayEquals(new String[] { TEST_URL },
                ((MockResourceLoader) registry.getResourceLoader()).loadingHtml
                        .toArray());
    }

    @Test
    public void loadMultiple() {
        String TEST_JS_URL = "http://foo.bar/baz.js";
        String TEST_JS_URL2 = "my.js";
        String TEST_CSS_URL = "https://x.yz/styles.css";

        DependencyLoader loader = new DependencyLoader(registry);

        JsonArray deps = Json.createArray();
        JsonObject dep = Json.createObject();
        dep.put(DependencyList.KEY_TYPE, DependencyList.TYPE_JAVASCRIPT);
        dep.put(DependencyList.KEY_URL, TEST_JS_URL);
        deps.set(0, dep);
        dep = Json.createObject();
        dep.put(DependencyList.KEY_TYPE, DependencyList.TYPE_JAVASCRIPT);
        dep.put(DependencyList.KEY_URL, TEST_JS_URL2);
        deps.set(1, dep);
        dep = Json.createObject();
        dep.put(DependencyList.KEY_TYPE, DependencyList.TYPE_STYLESHEET);
        dep.put(DependencyList.KEY_URL, TEST_CSS_URL);
        deps.set(2, dep);

        loader.loadDependencies(deps);
        MockResourceLoader resourceLoader = ((MockResourceLoader) registry
                .getResourceLoader());
        Assert.assertArrayEquals(new String[] { TEST_JS_URL, TEST_JS_URL2 },
                resourceLoader.loadingScripts.toArray());
        Assert.assertArrayEquals(new String[] { TEST_CSS_URL },
                resourceLoader.loadingStyles.toArray());
    }

}
