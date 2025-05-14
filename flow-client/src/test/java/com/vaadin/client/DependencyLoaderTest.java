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
package com.vaadin.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static org.junit.Assert.assertEquals;

/**
 * This class is used to test {@link DependencyLoader} functionality, that does
 * not require GWT.
 * <p>
 * For the rest of the tests, refer to {@link GwtDependencyLoaderTest}
 */
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

        @Override
        public void inlineHtml(String htmlContents,
                ResourceLoadListener resourceLoadListener) {
            loadingHtml.add(htmlContents);
            resourceLoadListener
                    .onLoad(new ResourceLoadEvent(this, htmlContents));
        }

        @Override
        public void inlineScript(String scriptContents,
                ResourceLoadListener resourceLoadListener) {
            loadingScripts.add(scriptContents);
            resourceLoadListener
                    .onLoad(new ResourceLoadEvent(this, scriptContents));
        }

        @Override
        public void inlineStyleSheet(String styleSheetContents,
                ResourceLoadListener resourceLoadListener) {
            loadingStyles.add(styleSheetContents);
            resourceLoadListener
                    .onLoad(new ResourceLoadEvent(this, styleSheetContents));
        }
    }

    private final MockResourceLoader mockResourceLoader = new MockResourceLoader();

    private final Registry registry = new Registry() {
        {
            set(ResourceLoader.class, mockResourceLoader);
            set(URIResolver.class, new URIResolver(this));
            ApplicationConfiguration appConf = new ApplicationConfiguration();
            appConf.setContextRootUrl("../");
            set(ApplicationConfiguration.class, appConf);
        }
    };

    @Test
    public void loadStylesheet() {
        String TEST_URL = "http://foo.bar/baz";

        new DependencyLoader(registry).loadDependencies(
                createDependenciesMap(new Dependency(Dependency.Type.STYLESHEET,
                        TEST_URL, LoadMode.EAGER).toJson()));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingStyles);
    }

    @Test
    public void loadScript() {
        String TEST_URL = "http://foo.bar/baz.js";

        new DependencyLoader(registry).loadDependencies(
                createDependenciesMap(new Dependency(Dependency.Type.JAVASCRIPT,
                        TEST_URL, LoadMode.EAGER).toJson()));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingScripts);
    }

    @Test
    public void loadMultiple() {
        String TEST_JS_URL = "http://foo.bar/baz.js";
        String TEST_JS_URL2 = "/my.js";
        String TEST_CSS_URL = "https://x.yz/styles.css";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                new Dependency(Dependency.Type.JAVASCRIPT, TEST_JS_URL,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.JAVASCRIPT, TEST_JS_URL2,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, TEST_CSS_URL,
                        LoadMode.EAGER).toJson()));

        assertEquals(Arrays.asList(TEST_JS_URL, TEST_JS_URL2),
                mockResourceLoader.loadingScripts);
        assertEquals(Collections.singletonList(TEST_CSS_URL),
                mockResourceLoader.loadingStyles);
    }

    @Test
    public void ensureEagerDependenciesLoadedInOrder() {
        String jsUrl1 = "/1.js";
        String jsUrl2 = "/2.js";
        String cssUrl1 = "/1.css";
        String cssUrl2 = "/2.css";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                new Dependency(Dependency.Type.JAVASCRIPT, jsUrl1,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.JAVASCRIPT, jsUrl2,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, cssUrl1,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, cssUrl2,
                        LoadMode.EAGER).toJson()));

        assertEquals(
                "jsUrl1 should come before jsUrl2, because it was added earlier",
                Arrays.asList(jsUrl1, jsUrl2),
                mockResourceLoader.loadingScripts);

        assertEquals(
                "cssUrl1 should come before cssUrl2, because it was added earlier",
                Arrays.asList(cssUrl1, cssUrl2),
                mockResourceLoader.loadingStyles);

    }

    @Test
    public void ensureInlineDependenciesLoadedInOrder() {
        String jsContents1 = "/1.js";
        String jsContents2 = "/2.js";
        String cssContents1 = "/1.css";
        String cssContents2 = "/2.css";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createInlineDependency(Dependency.Type.JAVASCRIPT, jsContents1),
                createInlineDependency(Dependency.Type.JAVASCRIPT, jsContents2),
                createInlineDependency(Dependency.Type.STYLESHEET,
                        cssContents1),
                createInlineDependency(Dependency.Type.STYLESHEET,
                        cssContents2)));

        assertEquals(
                "jsContents1 should come before jsContents2, because it was added earlier",
                Arrays.asList(jsContents1, jsContents2),
                mockResourceLoader.loadingScripts);

        assertEquals(
                "cssContents1 should come before cssContents2, because it was added earlier",
                Arrays.asList(cssContents1, cssContents2),
                mockResourceLoader.loadingStyles);

    }

    private JsMap<LoadMode, JsonArray> createDependenciesMap(
            JsonObject... dependencies) {
        JsMap<LoadMode, JsonArray> result = JsCollections.map();
        for (int i = 0; i < dependencies.length; i++) {
            JsonObject dependency = dependencies[i];
            LoadMode loadMode = LoadMode
                    .valueOf(dependency.getString(Dependency.KEY_LOAD_MODE));
            JsonArray jsonArray = Json.createArray();
            jsonArray.set(0, dependency);

            JsonArray oldResult = result.get(loadMode);
            if (oldResult == null) {
                result.set(loadMode, jsonArray);
            } else {
                mergeArrays(oldResult, jsonArray);
            }
        }
        return result;
    }

    private JsonArray mergeArrays(JsonArray jsonArray1, JsonArray jsonArray2) {
        for (int i = 0; i < jsonArray2.length(); i++) {
            jsonArray1.set(jsonArray1.length(), jsonArray2.getObject(i));
        }
        return jsonArray1;
    }

    private JsonObject createInlineDependency(Dependency.Type dependencyType,
            String contents) {
        JsonObject json = new Dependency(dependencyType, "", LoadMode.INLINE)
                .toJson();
        json.remove(Dependency.KEY_URL);
        json.put(Dependency.KEY_CONTENTS, contents);
        return json;
    }
}
