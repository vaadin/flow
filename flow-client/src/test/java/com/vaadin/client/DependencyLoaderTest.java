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
import java.util.List;

import org.junit.Test;

import com.vaadin.ui.DependencyList;

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

        new DependencyLoader(registry)
                .loadDependencies(createJsonArray(createDependency(TEST_URL,
                        DependencyList.TYPE_STYLESHEET, true)));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingStyles);
    }

    @Test
    public void loadScript() {
        String TEST_URL = "http://foo.bar/baz.js";

        new DependencyLoader(registry)
                .loadDependencies(createJsonArray(createDependency(TEST_URL,
                        DependencyList.TYPE_JAVASCRIPT, true)));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingScripts);
    }

    @Test
    public void loadHtml() {
        String TEST_URL = "http://foo.bar/baz.html";

        new DependencyLoader(registry)
                .loadDependencies(createJsonArray(createDependency(TEST_URL,
                        DependencyList.TYPE_HTML_IMPORT, true)));

        assertEquals(Collections.singletonList(TEST_URL),
                mockResourceLoader.loadingHtml);
    }

    @Test
    public void loadMultiple() {
        String TEST_JS_URL = "http://foo.bar/baz.js";
        String TEST_JS_URL2 = "my.js";
        String TEST_CSS_URL = "https://x.yz/styles.css";

        new DependencyLoader(registry).loadDependencies(createJsonArray(
                createDependency(TEST_JS_URL, DependencyList.TYPE_JAVASCRIPT,
                        true),
                createDependency(TEST_JS_URL2, DependencyList.TYPE_JAVASCRIPT,
                        true),
                createDependency(TEST_CSS_URL, DependencyList.TYPE_STYLESHEET,
                        true)));

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

        new DependencyLoader(registry)
                .loadDependencies(createJsonArray(createDependency(TEST_URL,
                        DependencyList.TYPE_HTML_IMPORT, true)));

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

        new DependencyLoader(registry)
                .loadDependencies(createJsonArray(createDependency(TEST_URL,
                        DependencyList.TYPE_HTML_IMPORT, true)));

        assertEquals(
                Collections.singletonList(
                        "http://someplace.com/es6/my-component.html"),
                mockResourceLoader.loadingHtml);
    }

    @Test
    public void allBlockingDependenciesAreLoadedFirst() {
        String blockingJsUrl = "https://foo.bar/blocking_script.js";
        String blockingHtmlUrl = "https://foo.bar/blocking_page.html";
        String blockingCssUrl = "https://foo.bar/blocking_style.css";

        String regularJsUrl = "https://foo.bar/script.js";
        String regularHtmlUrl = "https://foo.bar/page.html";
        String regularCssUrl = "https://foo.bar/style.css";

        new DependencyLoader(registry).loadDependencies(createJsonArray(
                createDependency(regularJsUrl, DependencyList.TYPE_JAVASCRIPT,
                        false),
                createDependency(regularHtmlUrl,
                        DependencyList.TYPE_HTML_IMPORT, false),
                createDependency(regularCssUrl, DependencyList.TYPE_STYLESHEET,
                        false),

                createDependency(blockingJsUrl, DependencyList.TYPE_JAVASCRIPT,
                        true),
                createDependency(blockingHtmlUrl,
                        DependencyList.TYPE_HTML_IMPORT, true),
                createDependency(blockingCssUrl, DependencyList.TYPE_STYLESHEET,
                        true)));

        assertEquals("2 js files should be imported, blocking first",
                Arrays.asList(blockingJsUrl, regularJsUrl),
                mockResourceLoader.loadingScripts);

        assertEquals("2 style files should be imported, blocking first",
                Arrays.asList(blockingCssUrl, regularCssUrl),
                mockResourceLoader.loadingStyles);

        assertEquals("2 html files should be imported, blocking first",
                Arrays.asList(blockingHtmlUrl, regularHtmlUrl),
                mockResourceLoader.loadingHtml);
    }

    @Test
    public void ensureNonBlockingDependenciesLoadedInOrder() {
        ensureDependenciesLoadedInOrder(false);
    }

    @Test
    public void ensureBlockingDependenciesLoadedInOrder() {
        ensureDependenciesLoadedInOrder(true);
    }

    private void ensureDependenciesLoadedInOrder(boolean blocking) {
        String jsUrl1 = "1.js";
        String jsUrl2 = "2.js";
        String cssUrl1 = "1.css";
        String cssUrl2 = "2.css";
        String htmlUrl1 = "1.html";
        String htmlUrl2 = "2.html";

        new DependencyLoader(registry).loadDependencies(createJsonArray(
                createDependency(jsUrl1, DependencyList.TYPE_JAVASCRIPT,
                        blocking),
                createDependency(jsUrl2, DependencyList.TYPE_JAVASCRIPT,
                        blocking),
                createDependency(cssUrl1, DependencyList.TYPE_STYLESHEET,
                        blocking),
                createDependency(cssUrl2, DependencyList.TYPE_STYLESHEET,
                        blocking),
                createDependency(htmlUrl1, DependencyList.TYPE_HTML_IMPORT,
                        blocking),
                createDependency(htmlUrl2, DependencyList.TYPE_HTML_IMPORT,
                        blocking)));

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

    private JsonObject createDependency(String url, String type,
            boolean blocking) {
        JsonObject dependency = Json.createObject();
        dependency.put(DependencyList.KEY_TYPE, type);
        dependency.put(DependencyList.KEY_URL, url);
        dependency.put(DependencyList.KEY_BLOCKING, blocking);
        return dependency;
    }

    private JsonArray createJsonArray(JsonObject... contents) {
        JsonArray result = Json.createArray();
        for (int i = 0; i < contents.length; i++) {
            result.set(i, contents[i]);
        }
        return result;
    }
}
