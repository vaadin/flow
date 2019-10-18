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
package com.vaadin.flow.server.startup;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servlet initializer for collecting all classes that extend {@link
 * WebComponentExporter} on startup, creates unique
 * {@link WebComponentConfiguration} instances, and adds them to
 * {@link WebComponentConfigurationRegistry}.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
@HandlesTypes({ExportsWebComponent.class})
public class WebComponentConfigurationRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    @SuppressWarnings("unchecked")
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        WebComponentConfigurationRegistry instance = WebComponentConfigurationRegistry
                .getInstance(new VaadinServletContext(servletContext));

        if (set == null || set.isEmpty()) {
            instance.setConfigurations(Collections.emptySet());
            return;
        }

        try {
            Set<Class<? extends ExportsWebComponent<? extends Component>>> exporterClasses = set
                    .stream().filter(ExportsWebComponent.class::isAssignableFrom)
                    .filter(clazz -> !clazz.isInterface()
                            && !Modifier.isAbstract(clazz.getModifiers()))
                    .map(aClass -> (Class<? extends ExportsWebComponent<? extends Component>>) aClass)
                    .collect(Collectors.toSet());

            Set<WebComponentConfiguration<? extends Component>> configurations =
                    constructConfigurations(exporterClasses);

            validateTagNames(configurations);
            validateDistinctTagNames(configurations);

            instance.setConfigurations(configurations);
        } catch (Exception e) {
            throw new ServletException(String.format(
                    "%s failed to collect %s implementations!",
                    WebComponentConfigurationRegistryInitializer.class.getSimpleName(),
                    WebComponentExporter.class.getSimpleName()),
                    e);
        }
    }

    private static Set<WebComponentConfiguration<? extends Component>> constructConfigurations(
            Set<Class<? extends ExportsWebComponent<? extends Component>>> exporterClasses) {
        Objects.requireNonNull(exporterClasses, "Parameter 'exporterClasses' " +
                "cannot be null!");

        final WebComponentExporter.WebComponentConfigurationFactory factory =
                new WebComponentExporter.WebComponentConfigurationFactory();

        return exporterClasses.stream().map(factory::create)
                .collect(Collectors.toSet());
    }

    /**
     * Validate that all web component names are valid custom element names.
     *
     * @param configurationSet
     *         set of web components to validate
     */
    private static void validateTagNames(
            Set<WebComponentConfiguration<? extends Component>> configurationSet) {
        for (WebComponentConfiguration<? extends Component> configuration :
                configurationSet) {
            if (!CustomElementNameValidator
                    .isCustomElementName(configuration.getTag())) {
                throw new InvalidCustomElementNameException(String.format(
                        "Tag name '%s' given by '%s' is not a valid custom "
                                + "element name.",
                        configuration.getTag(),
                        configuration.getExporterClass().getCanonicalName()));
            }
        }
    }

    /**
     * Validate that we have exactly one {@link WebComponentConfiguration} per
     * tag name.
     *
     * @param configurationSet
     *         set of web components to validate
     */
    private static void validateDistinctTagNames(
            Set<WebComponentConfiguration<? extends Component>> configurationSet) {
        long count = configurationSet.stream()
                .map(WebComponentConfiguration::getTag)
                .distinct().count();
        if (configurationSet.size() != count) {
            Map<String, WebComponentConfiguration<? extends Component>> items =
                    new HashMap<>();
            for (WebComponentConfiguration<? extends Component> configuration :
                    configurationSet) {
                String tag = configuration.getTag();
                if (items.containsKey(tag)) {
                    String message = String.format(
                            "Found two %s classes '%s' and '%s' for the tag "
                                    + "name '%s'. Tag must be unique.",
                            WebComponentExporter.class.getSimpleName(),
                            items.get(tag).getExporterClass().getCanonicalName(),
                            configuration.getExporterClass().getCanonicalName(),
                            tag);
                    throw new IllegalArgumentException(message);
                }
                items.put(tag, configuration);
            }
        }
    }
}
