/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.polymertemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.startup.FakeBrowser;
import com.vaadin.flow.shared.ui.Dependency;

import elemental.json.JsonObject;

/**
 * Npm template parser implementation.
 * <p>
 * The implementation scans all JsModule annotations for the given template
 * class and tries to find the one that contains template definition using the
 * tag name.
 * <p>
 * The class is Singleton. Use {@link DefaultTemplateParser#getInstance()} to
 * get its instance.
 *
 * @author Vaadin Ltd
 * @see BundleParser
 * @since
 */
public class NpmTemplateParser implements TemplateParser {

    private static final TemplateParser INSTANCE = new NpmTemplateParser();

    private HashMap<String, String> cache = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock();
    private JsonObject jsonStats;

    private NpmTemplateParser() {
        // Doesn't allow external instantiation
    }

    public static TemplateParser getInstance() {
        return INSTANCE;
    }

    @Override
    public TemplateData getTemplateContent(Class<? extends PolymerTemplate<?>> clazz, String tag,
            VaadinService service) {

        WebBrowser browser = FakeBrowser.getEs6();

        List<Dependency> dependencies = AnnotationReader.getAnnotationsFor(clazz, JsModule.class).stream()
                .map(htmlImport -> new Dependency(Dependency.Type.JS_MODULE, htmlImport.value(), htmlImport.loadMode()))
                .collect(Collectors.toList());

        DependencyFilter.FilterContext filterContext = new DependencyFilter.FilterContext(service, browser);
        for (DependencyFilter filter : service.getDependencyFilters()) {
            dependencies = filter.filter(new ArrayList<>(dependencies), filterContext);
        }

        for (Dependency dependency : dependencies) {
            if (dependency.getType() != Dependency.Type.JS_MODULE) {
                continue;
            }

            String url = dependency.getUrl();
            String source = getSourcesFromTemplate(tag, url);
            if (source == null) {
                source = getSourcesFromStats(service, tag, url);
            }

            if (source != null) {
                // Template needs to be wrapped in an element with id, to look like a P2 template
                Element parent = new Element(tag);
                parent.attr("id", tag);

                Element templateElement = BundleParser.parseTemplateElement(url, source);
                templateElement.appendTo(parent);

                return new TemplateData(url, templateElement);
            }
        }

        throw new IllegalStateException(String.format("Couldn't find the " + "definition of the element with tag '%s' "
                + "in any template file declared using '@%s' annotations. "
                + "Check the availability of the template files in your WAR "
                + "file or provide alternative implementation of the "
                + "method getTemplateContent() which should return an element "
                + "representing the content of the template file", tag, JsModule.class.getSimpleName()));
    }

    private String getSourcesFromTemplate(String tag, String url) {
        InputStream content = getClass().getClassLoader().getResourceAsStream(url);
        if (content != null) {
            getLogger().debug("Found sources from the tag '{}' in '{}'", tag, url);
            return streamToString(content);
        }
        return null;
    }

    private String getSourcesFromStats(VaadinService service, String tag, String url) {
        String stats = service.getDeploymentConfiguration()
                .getStringProperty(Constants.STATISTICS_JSON, Constants.STATISTICS_JSON_DEFAULT)
                // Remove absolute
                .replaceFirst("^/", "");

        try {
            // Read from filesystem, this is for production or in devmode when
            // webpack outputs to the webapp folder
            URL statsUrl = service.getStaticResource("/" + stats);

            // Otherwise, stats file is not accessible by flow, then ask webpack
            // via http
            if (statsUrl == null && !service.getDeploymentConfiguration().isProductionMode()) {
                String port = System.getProperty(DevModeHandler.PARAM_WEBPACK_RUNNING);
                if (port != null) {
                    statsUrl = new URL("http://localhost:" + port + "/" + stats);
                }
            }

            if (statsUrl != null) {
                statsUrl.openConnection();
                updateCache(url, streamToString(statsUrl.openStream()));
                getLogger().debug("Found sources for the tag '{}' in '{}'", tag, stats);
                return cache.get(url);

            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return null;
    }

    private void updateCache(String url, String fileContents) {
        if (jsonStats == null || !jsonStats.getString("hash").equals(BundleParser.getHashFromStatistics(fileContents))) {
            cache.clear();
            try {
                lock.lock();
                jsonStats = BundleParser.parseJsonStatistics(fileContents);
            } finally {
                lock.unlock();
            }
        }
        if (!cache.containsKey(url)) {
            cache.put(url, BundleParser.getSourceFromStatistics(url, jsonStats));
        }
    }

    private String streamToString(InputStream inputStream) {
        String ret = "";
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8.name()))) {

            ret = br.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException exception) {
            // ignore exception on close()
            getLogger().warn("Couldn't close template input stream", exception);
        }
        return ret;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(NpmTemplateParser.class.getName());
    }
}
