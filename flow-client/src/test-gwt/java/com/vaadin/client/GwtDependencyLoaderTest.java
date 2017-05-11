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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.impl.SchedulerImpl;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.DependencyList;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class GwtDependencyLoaderTest extends ClientEngineTestBase {

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

    public static class CustomScheduler extends SchedulerImpl {

        @Override
        public void scheduleDeferred(ScheduledCommand cmd) {
            cmd.execute();
        }
    }

    private MockResourceLoader mockResourceLoader;

    private Registry registry;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        mockResourceLoader = new MockResourceLoader();
        registry = new Registry() {
            {
                set(ResourceLoader.class, mockResourceLoader);
                set(URIResolver.class, new URIResolver(this));
            }
        };
        initScheduler(new CustomScheduler());
    }

    public void testAllEagerDependenciesAreLoadedFirst() {
        String eagerJsUrl = "https://foo.bar/eager_script.js";
        String eagerHtmlUrl = "https://foo.bar/eager_page.html";
        String eagerCssUrl = "https://foo.bar/eager_style.css";

        String lazyJsUrl = "https://foo.bar/script.js";
        String lazyHtmlUrl = "https://foo.bar/page.html";
        String lazyCssUrl = "https://foo.bar/style.css";

        new DependencyLoader(registry).loadDependencies(createJsonArray(
                createDependency(lazyJsUrl, DependencyList.TYPE_JAVASCRIPT,
                        LoadMode.LAZY),
                createDependency(lazyHtmlUrl,
                        DependencyList.TYPE_HTML_IMPORT, LoadMode.LAZY),
                createDependency(lazyCssUrl, DependencyList.TYPE_STYLESHEET,
                        LoadMode.LAZY),

                createDependency(eagerJsUrl, DependencyList.TYPE_JAVASCRIPT,
                        LoadMode.EAGER),
                createDependency(eagerHtmlUrl,
                        DependencyList.TYPE_HTML_IMPORT, LoadMode.EAGER),
                createDependency(eagerCssUrl, DependencyList.TYPE_STYLESHEET,
                        LoadMode.EAGER)));

        assertEquals(Arrays.asList(eagerJsUrl, lazyJsUrl),
                mockResourceLoader.loadingScripts);

        assertEquals("2 style files should be imported, eager first",
                Arrays.asList(eagerCssUrl, lazyCssUrl),
                mockResourceLoader.loadingStyles);

        assertEquals("2 html files should be imported, eager first",
                Arrays.asList(eagerHtmlUrl, lazyHtmlUrl),
                mockResourceLoader.loadingHtml);
    }

    public void testEnsureLazyDependenciesLoadedInOrder() {
        String jsUrl1 = "1.js";
        String jsUrl2 = "2.js";
        String cssUrl1 = "1.css";
        String cssUrl2 = "2.css";
        String htmlUrl1 = "1.html";
        String htmlUrl2 = "2.html";

        new DependencyLoader(registry).loadDependencies(createJsonArray(
                createDependency(jsUrl1, DependencyList.TYPE_JAVASCRIPT, LoadMode.LAZY),
                createDependency(jsUrl2, DependencyList.TYPE_JAVASCRIPT, LoadMode.LAZY),
                createDependency(cssUrl1, DependencyList.TYPE_STYLESHEET,
                        LoadMode.LAZY),
                createDependency(cssUrl2, DependencyList.TYPE_STYLESHEET,
                        LoadMode.LAZY),
                createDependency(htmlUrl1, DependencyList.TYPE_HTML_IMPORT,
                        LoadMode.LAZY),
                createDependency(htmlUrl2, DependencyList.TYPE_HTML_IMPORT,
                        LoadMode.LAZY)));

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

    private JsonArray createJsonArray(JsonObject... contents) {
        JsonArray result = Json.createArray();
        for (int i = 0; i < contents.length; i++) {
            result.set(i, contents[i]);
        }
        return result;
    }

    private JsonObject createDependency(String url, String type,
                                        LoadMode loadMode) {
        JsonObject dependency = Json.createObject();
        dependency.put(DependencyList.KEY_TYPE, type);
        dependency.put(DependencyList.KEY_URL, url);
        dependency.put(DependencyList.KEY_LOAD_MODE, loadMode.name());
        return dependency;
    }

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;
}
