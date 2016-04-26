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
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.hummingbird.nodefeature.DependencyList;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.json.Json;
import elemental.json.JsonObject;

public class DependencyLoaderTest {

    private Registry registry = new Registry();
    StateTree tree = new StateTree(registry);
    StateNode rootNode = tree.getRootNode();

    private static class MockDependencyLoader extends DependencyLoader {
        List<String> loadingStyles = new ArrayList<>();
        List<String> loadingScripts = new ArrayList<>();

        public MockDependencyLoader(Registry registry) {
            super(registry);
        }

        @Override
        public void loadStyleDependencies(JsArray<String> dependencies) {
            for (int i = 0; i < dependencies.length(); i++) {
                loadingStyles.add(dependencies.get(i));
            }
        }

        @Override
        public void loadScriptDependencies(JsArray<String> dependencies) {
            for (int i = 0; i < dependencies.length(); i++) {
                loadingScripts.add(dependencies.get(i));
            }
        }
    }

    @Test
    public void loadStylesheet() {
        String TEST_URL = "http://foo.bar/baz";
        MockDependencyLoader loader = new MockDependencyLoader(registry);
        DependencyLoader.bind(loader, rootNode);

        JsonObject styleDep = Json.createObject();
        styleDep.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_STYLESHEET);
        styleDep.put(DependencyList.KEY_URL, TEST_URL);
        rootNode.getList(NodeFeatures.DEPENDENCY_LIST).add(0, styleDep);
        Assert.assertArrayEquals(new String[] { TEST_URL },
                loader.loadingStyles.toArray());
    }

    @Test
    public void loadScript() {
        String TEST_URL = "http://foo.bar/baz.js";

        MockDependencyLoader loader = new MockDependencyLoader(registry);
        DependencyLoader.bind(loader, rootNode);

        JsonObject styleDep = Json.createObject();
        styleDep.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_JAVASCRIPT);
        styleDep.put(DependencyList.KEY_URL, TEST_URL);
        rootNode.getList(NodeFeatures.DEPENDENCY_LIST).add(0, styleDep);
        Assert.assertArrayEquals(new String[] { TEST_URL },
                loader.loadingScripts.toArray());
    }

    @Test
    public void loadMultiple() {
        String TEST_JS_URL = "http://foo.bar/baz.js";
        String TEST_JS_URL2 = "my.js";
        String TEST_CSS_URL = "https://x.yz/styles.css";

        MockDependencyLoader loader = new MockDependencyLoader(registry);
        DependencyLoader.bind(loader, rootNode);

        JsArray<JsonObject> add = JsCollections.array();
        JsonObject dep = Json.createObject();
        dep.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_JAVASCRIPT);
        dep.put(DependencyList.KEY_URL, TEST_JS_URL);
        add.push(dep);
        dep = Json.createObject();
        dep.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_JAVASCRIPT);
        dep.put(DependencyList.KEY_URL, TEST_JS_URL2);
        add.push(dep);
        dep = Json.createObject();
        dep.put(DependencyList.KEY_TYPE,
                DependencyList.TYPE_STYLESHEET);
        dep.put(DependencyList.KEY_URL, TEST_CSS_URL);
        add.push(dep);
        rootNode.getList(NodeFeatures.DEPENDENCY_LIST).splice(0, 0, add);

        Assert.assertArrayEquals(new String[] { TEST_JS_URL, TEST_JS_URL2 },
                loader.loadingScripts.toArray());
        Assert.assertArrayEquals(new String[] { TEST_CSS_URL },
                loader.loadingStyles.toArray());
    }

}
