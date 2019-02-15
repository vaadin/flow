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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.startup.FakeBrowser;
import com.vaadin.flow.shared.ui.Dependency;

import elemental.json.JsonObject;

public class NpmTemplateParser implements TemplateParser {

    private static final TemplateParser INSTANCE = new NpmTemplateParser();

    private String hash;
    private JsonObject statisticsJson;
    private ReentrantLock lock = new ReentrantLock();

    private static final Pattern HASH_PATTERN = Pattern
            .compile("\"hash\":\\s\"(.*)\",");

    private NpmTemplateParser() {
        // Doesn't allow external instantiation
    }

    public static TemplateParser getInstance() {
        return INSTANCE;
    }

    @Override
    public TemplateData getTemplateContent(
            Class<? extends PolymerTemplate<?>> clazz, String tag,
            VaadinService service) {

        WebBrowser browser = FakeBrowser.getEs6();

        List<Dependency> dependencies = AnnotationReader
                .getAnnotationsFor(clazz, JavaScript.class).stream()
                .map(htmlImport -> new Dependency(Dependency.Type.JAVASCRIPT,
                        htmlImport.value(), htmlImport.loadMode()))
                .collect(Collectors.toList());

        DependencyFilter.FilterContext filterContext = new DependencyFilter.FilterContext(
                service, browser);
        for (DependencyFilter filter : service.getDependencyFilters()) {
            dependencies = filter
                    .filter(new ArrayList<>(dependencies), filterContext);
        }

        boolean productionMode = service.getDeploymentConfiguration()
                .isProductionMode();

        for (Dependency dependency : dependencies) {
            if (dependency.getType() != Dependency.Type.JAVASCRIPT) {
                continue;
            }
            String url = dependency.getUrl();
            String sourcesFilePath;

            if (productionMode) {
                sourcesFilePath = service.getDeploymentConfiguration()
                        .getStringProperty(Constants.STATISTICS_JSON,
                                "META-INF/resources/stats.json");
            } else {
                sourcesFilePath = url;
            }

            try (InputStream content = getClass().getClassLoader()
                    .getResourceAsStream(sourcesFilePath)) {
                if (content == null) {
                    throw new IllegalStateException(String.format(
                            "Can't find resource '%s' "
                                    + "via the servlet context", url));
                }

                String fileContents = streamToString(content);
                Element templateElement;

                if (productionMode) {
                    Matcher matcher = HASH_PATTERN.matcher(fileContents);
                    if (matcher.find()) {
                        String hash = matcher.group(1);
                        lock.lock();
                        try {
                            if (!hash.equals(this.hash)) {
                                this.hash = hash;
                                statisticsJson = BundleParser
                                        .getStatisticsJson(url, fileContents);
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        statisticsJson = BundleParser
                                .getStatisticsJson(url, fileContents);
                    }

                    templateElement = BundleParser
                            .parseTemplateElement(statisticsJson);
                } else {
                    templateElement = BundleParser
                            .parseTemplateElement(url, fileContents);
                }

                Element parent = new Element(tag);
                parent.attr("id", tag);
                templateElement.appendTo(parent);

                if (templateElement != null) {
                    getLogger()
                            .debug("Found a template file containing template definition for the tag '{}' by the path '{}'",
                                    tag, url);
                }

                if (templateElement != null) {
                    return new TemplateData(url, templateElement);

                }
            } catch (IOException exception) {
                // ignore exception on close()
                getLogger().warn("Couldn't close template input stream",
                        exception);
            }
        }
        throw new IllegalStateException(String.format("Couldn't find the "
                        + "definition of the element with tag '%s' "
                        + "in any template file declared using @'%s' annotations. "
                        + "Check the availability of the template files in your WAR "
                        + "file or provide alternative implementation of the "
                        + "method getTemplateContent() which should return an element "
                        + "representing the content of the template file", tag,
                JavaScript.class.getSimpleName()));
    }

    private String streamToString(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream,
                        StandardCharsets.UTF_8.name()))) {
            return br.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(NpmTemplateParser.class.getName());
    }
}
