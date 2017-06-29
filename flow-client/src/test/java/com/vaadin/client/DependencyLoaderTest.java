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
package com.vaadin.client;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class DependencyLoaderTest {

    private static class MockResourceLoader extends ResourceLoader {
        private MockResourceLoader() {
            super(new Registry(), false);
        }

        List<String> loadingStyles = new ArrayList<>();
        List<String> loadingScripts = new ArrayList<>();
        List<String> loadingHtml = new ArrayList<>();

        @Override
        public void loadHtml(String htmlUrl,
                ResourceLoadListener resourceLoadListener, boolean async) {
            loadingHtml.add(htmlUrl);
            resourceLoadListener.onLoad(new ResourceLoadEvent(this, htmlUrl));
        }

        @Override
        public void loadScript(String scriptUrl,
                ResourceLoadListener resourceLoadListener) {
            loadingScripts.add(scriptUrl);
            resourceLoadListener.onLoad(new ResourceLoadEvent(this, scriptUrl));
        }

        @Override
        public void loadScript(String scriptUrl,
                ResourceLoadListener resourceLoadListener, boolean async,
                boolean defer) {
            loadingScripts.add(scriptUrl);
            resourceLoadListener.onLoad(new ResourceLoadEvent(this, scriptUrl));
        }

        @Override
        public void loadStylesheet(String stylesheetUrl,
                ResourceLoadListener resourceLoadListener) {
            loadingStyles.add(stylesheetUrl);
            resourceLoadListener
                    .onLoad(new ResourceLoadEvent(this, stylesheetUrl));
        }
    }

    private final MockResourceLoader mockResourceLoader = new MockResourceLoader();

    private final Registry registry = new Registry() {
        {
            set(ResourceLoader.class, mockResourceLoader);
            set(URIResolver.class, new URIResolver(this));
        }
    };

    @Test
    public void loadStylesheet() {
        String TEST_URL = "http://foo.bar/baz";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(TEST_URL, Dependency.Type.STYLESHEET)));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingStyles);
    }

    @Test
    public void loadScript() {
        String TEST_URL = "http://foo.bar/baz.js";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(TEST_URL, Dependency.Type.JAVASCRIPT)));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingScripts);
    }

    @Test
    public void loadHtml() {
        String TEST_URL = "http://foo.bar/baz.html";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(TEST_URL, Dependency.Type.HTML_IMPORT)));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingHtml);
    }

    @Test
    public void loadMultiple() {
        String TEST_JS_URL = "http://foo.bar/baz.js";
        String TEST_JS_URL2 = "my.js";
        String TEST_CSS_URL = "https://x.yz/styles.css";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(TEST_JS_URL, Dependency.Type.JAVASCRIPT),
                createDependency(TEST_JS_URL2, Dependency.Type.JAVASCRIPT),
                createDependency(TEST_CSS_URL,
                        Dependency.Type.STYLESHEET)));

        assertEquals(Arrays.asList(TEST_JS_URL, TEST_JS_URL2),
                mockResourceLoader.loadingScripts);
        assertEquals(Collections.singletonList(TEST_CSS_URL),
                mockResourceLoader.loadingStyles);
    }

    @Test
    public void loadFrontendDependency() {
        String TEST_URL = "frontend://my-component.html";

        ApplicationConfiguration config = new ApplicationConfiguration();

        registry.set(ApplicationConfiguration.class, config);
        config.setFrontendRootUrl("http://someplace.com/es6/");

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(TEST_URL, Dependency.Type.HTML_IMPORT)));

        assertEquals(
                Collections.singletonList(
                        "http://someplace.com/es6/my-component.html"),
                mockResourceLoader.loadingHtml);
    }

    @Test
    public void loadFrontendDependencyWithContext() {
        String TEST_URL = "frontend://my-component.html";

        ApplicationConfiguration config = new ApplicationConfiguration();

        registry.set(ApplicationConfiguration.class, config);
        config.setFrontendRootUrl("context://es6/");
        config.setContextRootUrl("http://someplace.com/");

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(TEST_URL, Dependency.Type.HTML_IMPORT)));

        assertEquals(
                Collections.singletonList(
                        "http://someplace.com/es6/my-component.html"),
                mockResourceLoader.loadingHtml);
    }

    @Test
    public void ensureEagerDependenciesLoadedInOrder() {
        ensureDependenciesLoadedInOrder();
    }

    private void ensureDependenciesLoadedInOrder() {
        String jsUrl1 = "1.js";
        String jsUrl2 = "2.js";
        String cssUrl1 = "1.css";
        String cssUrl2 = "2.css";
        String htmlUrl1 = "1.html";
        String htmlUrl2 = "2.html";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createDependency(jsUrl1, Dependency.Type.JAVASCRIPT),
                createDependency(jsUrl2, Dependency.Type.JAVASCRIPT),
                createDependency(cssUrl1, Dependency.Type.STYLESHEET),
                createDependency(cssUrl2, Dependency.Type.STYLESHEET),
                createDependency(htmlUrl1, Dependency.Type.HTML_IMPORT),
                createDependency(htmlUrl2, Dependency.Type.HTML_IMPORT)));

        assertEquals(
                "jsUrl1 should come before jsUrl2, because it was added earlier",
                Arrays.asList(jsUrl1, jsUrl2),
                mockResourceLoader.loadingScripts);

        assertEquals(
                "cssUrl1 should come before cssUrl2, because it was added earlier",
                Arrays.asList(cssUrl1, cssUrl2),
                mockResourceLoader.loadingStyles);

        assertEquals(
                "htmlUrl1 should come before htmlUrl2, because it was added earlier",
                Arrays.asList(htmlUrl1, htmlUrl2),
                mockResourceLoader.loadingHtml);
    }

    private JsonObject createDependency(String url, Dependency.Type type) {
        JsonObject dependency = Json.createObject();
        dependency.put(Dependency.KEY_TYPE, type.name());
        dependency.put(Dependency.KEY_URL, url);
        dependency.put(Dependency.KEY_LOAD_MODE, LoadMode.EAGER.name());
        return dependency;
    }

    private Map<LoadMode, JsonArray> createDependenciesMap(JsonObject... dependencies) {
        Map<LoadMode, JsonArray> result = new HashMap<>();
        for (int i = 0; i < dependencies.length; i++) {
            JsonObject dependency = dependencies[i];
            LoadMode loadMode = LoadMode.valueOf(dependency.getString(Dependency.KEY_LOAD_MODE));
            JsonArray jsonArray = Json.createArray();
            jsonArray.set(0, dependency);
            result.merge(loadMode, jsonArray, this::mergeArrays);
        }
        return result;
    }

    private JsonArray mergeArrays(JsonArray jsonArray1, JsonArray jsonArray2) {
        for (int i = 0; i < jsonArray2.length(); i++) {
            jsonArray1.set(jsonArray1.length(), jsonArray2.getObject(i));
        }
        return jsonArray1;
    }
}
