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
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.startup.FakeBrowser;
import com.vaadin.flow.shared.ui.Dependency;

public class NpmTemplateParser implements TemplateParser {

    private static final TemplateParser INSTANCE = new NpmTemplateParser();

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

        //        boolean logEnabled = LOG_CACHE.get(clazz).compareAndSet(false, true);
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

        for (Dependency dependency : dependencies) {
            if (dependency.getType() != Dependency.Type.JAVASCRIPT) {
                continue;
            }
            String url = dependency.getUrl();
            String statisticsFilePath = service.getDeploymentConfiguration()
                    .getStringProperty(Constants.STATISTICS_JSON,
                            "META-INF/resources/stats.json");
            try (InputStream content = getClass().getClassLoader()
                    .getResourceAsStream(statisticsFilePath)) {
                if (content == null) {
                    throw new IllegalStateException(String.format(
                            "Can't find resource '%s' "
                                    + "via the servlet context", url));
                }
                Element templateElement = BundleParser
                        .parseTemplateElement(url, streamToString(content));
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
                HtmlImport.class.getSimpleName()));
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
