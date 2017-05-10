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

    public void testAllBlockingDependenciesAreLoadedFirst() {
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

        assertEquals(Arrays.asList(blockingJsUrl, regularJsUrl),
                mockResourceLoader.loadingScripts);

        assertEquals("2 style files should be imported, blocking first",
                Arrays.asList(blockingCssUrl, regularCssUrl),
                mockResourceLoader.loadingStyles);

        assertEquals("2 html files should be imported, blocking first",
                Arrays.asList(blockingHtmlUrl, regularHtmlUrl),
                mockResourceLoader.loadingHtml);
    }

    public void testEnsureNonBlockingDependenciesLoadedInOrder() {
        String jsUrl1 = "1.js";
        String jsUrl2 = "2.js";
        String cssUrl1 = "1.css";
        String cssUrl2 = "2.css";
        String htmlUrl1 = "1.html";
        String htmlUrl2 = "2.html";

        new DependencyLoader(registry).loadDependencies(createJsonArray(
                createDependency(jsUrl1, DependencyList.TYPE_JAVASCRIPT, false),
                createDependency(jsUrl2, DependencyList.TYPE_JAVASCRIPT, false),
                createDependency(cssUrl1, DependencyList.TYPE_STYLESHEET,
                        false),
                createDependency(cssUrl2, DependencyList.TYPE_STYLESHEET,
                        false),
                createDependency(htmlUrl1, DependencyList.TYPE_HTML_IMPORT,
                        false),
                createDependency(htmlUrl2, DependencyList.TYPE_HTML_IMPORT,
                        false)));

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
            boolean blocking) {
        JsonObject dependency = Json.createObject();
        dependency.put(DependencyList.KEY_TYPE, type);
        dependency.put(DependencyList.KEY_URL, url);
        dependency.put(DependencyList.KEY_BLOCKING, blocking);
        return dependency;
    }

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;
}
