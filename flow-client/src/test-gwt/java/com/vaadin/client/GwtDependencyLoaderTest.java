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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.core.client.impl.SchedulerImpl;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * This class is used to test {@link DependencyLoader} GWT functionality, that
 * is required to process dependencies with load mode {@link LoadMode#LAZY}.
 * <p>
 * For the rest of the tests, refer to {@link DependencyLoaderTest}
 */
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
                ApplicationConfiguration appConf = new ApplicationConfiguration();
                appConf.setContextRootUrl("../");
                appConf.setFrontendRootUrl("/frontend/");
                set(ApplicationConfiguration.class, appConf);
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

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                new Dependency(Dependency.Type.JAVASCRIPT, lazyJsUrl,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.HTML_IMPORT, lazyHtmlUrl,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, lazyCssUrl,
                        LoadMode.LAZY).toJson(),

                new Dependency(Dependency.Type.JAVASCRIPT, eagerJsUrl,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.HTML_IMPORT, eagerHtmlUrl,
                        LoadMode.EAGER).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, eagerCssUrl,
                        LoadMode.EAGER).toJson()));

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
        String jsUrl1 = "/1.js";
        String jsUrl2 = "/2.js";
        String cssUrl1 = "/1.css";
        String cssUrl2 = "/2.css";
        String htmlUrl1 = "/1.html";
        String htmlUrl2 = "/2.html";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                new Dependency(Dependency.Type.JAVASCRIPT, jsUrl1,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.JAVASCRIPT, jsUrl2,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, cssUrl1,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, cssUrl2,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.HTML_IMPORT, htmlUrl1,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.HTML_IMPORT, htmlUrl2,
                        LoadMode.LAZY).toJson()));

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

    public void testDependenciesWithAllLoadModesAreProcessed() {
        String eagerJsUrl = "/eager.js";
        String lazyJsUrl = "/lazy.js";
        String inlineJsContents = "/inline.js";

        String eagerCssUrl = "/eager.css";
        String lazyCssUrl = "/lazy.css";
        String inlineCssContents = "/inline.css";

        String eagerHtmlUrl = "/eager.html";
        String lazyHtmlUrl = "/lazy.html";
        String inlineHtmlContents = "/inline.html";

        new DependencyLoader(registry).loadDependencies(createDependenciesMap(
                createInlineDependency(Dependency.Type.JAVASCRIPT,
                        inlineJsContents),
                new Dependency(Dependency.Type.JAVASCRIPT, lazyJsUrl,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.JAVASCRIPT, eagerJsUrl,
                        LoadMode.EAGER).toJson(),

                createInlineDependency(Dependency.Type.STYLESHEET,
                        inlineCssContents),
                new Dependency(Dependency.Type.STYLESHEET, lazyCssUrl,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.STYLESHEET, eagerCssUrl,
                        LoadMode.EAGER).toJson(),

                createInlineDependency(Dependency.Type.HTML_IMPORT,
                        inlineHtmlContents),
                new Dependency(Dependency.Type.HTML_IMPORT, lazyHtmlUrl,
                        LoadMode.LAZY).toJson(),
                new Dependency(Dependency.Type.HTML_IMPORT, eagerHtmlUrl,
                        LoadMode.EAGER).toJson()));

        // When multiple LoadModes are used, no guarantees on the order can be
        // made except
        // for the fact that the last dependencies to be loaded are the lazy
        // ones
        assertEquals("All type of dependencies should be added",
                Stream.of(eagerJsUrl, inlineJsContents, lazyJsUrl)
                        .collect(Collectors.toSet()),
                new HashSet<>(mockResourceLoader.loadingScripts));

        assertEquals("All type of dependencies should be added",
                Stream.of(eagerCssUrl, inlineCssContents, lazyCssUrl)
                        .collect(Collectors.toSet()),
                new HashSet<>(mockResourceLoader.loadingStyles));

        assertEquals("All type of dependencies should be added",
                Stream.of(eagerHtmlUrl, inlineHtmlContents, lazyHtmlUrl)
                        .collect(Collectors.toSet()),
                new HashSet<>(mockResourceLoader.loadingHtml));
    }

    private Map<LoadMode, JsonArray> createDependenciesMap(
            JsonObject... dependencies) {
        Map<LoadMode, JsonArray> result = new EnumMap<>(LoadMode.class);
        for (int i = 0; i < dependencies.length; i++) {
            JsonObject dependency = dependencies[i];
            LoadMode loadMode = LoadMode
                    .valueOf(dependency.getString(Dependency.KEY_LOAD_MODE));
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

    private JsonObject createInlineDependency(Dependency.Type dependencyType,
            String contents) {
        JsonObject json = new Dependency(dependencyType, "", LoadMode.INLINE)
                .toJson();
        json.remove(Dependency.KEY_URL);
        json.put(Dependency.KEY_CONTENTS, contents);
        return json;
    }

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;
}
