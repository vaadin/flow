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

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

/**
 *
 * Servlet initializer for collecting all classes that extend
 * {@link WebComponentExporter} on startup.
 *
 * @author Vaadin Ltd.
 */
@HandlesTypes({ WebComponentExporter.class })
public class WebComponentConfigurationRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        WebComponentConfigurationRegistry instance = WebComponentConfigurationRegistry
                .getInstance(servletContext);

        if (set == null || set.isEmpty()) {
            instance.setExporters(Collections.emptyMap());
            return;
        }

        Set<Class<? extends WebComponentExporter<? extends Component>>> exporterClasses = set
                .stream().filter(WebComponentExporter.class::isAssignableFrom)
                .filter(clazz -> !clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers()))
                .map(aClass -> (Class<? extends WebComponentExporter<? extends Component>>) aClass)
                .collect(Collectors.toSet());

        validateDistinct(exporterClasses);

        Map<String, Class<? extends WebComponentExporter<? extends Component>>> exporterMap = exporterClasses
                .stream()
                .collect(Collectors.toMap(
                        WebComponentConfigurationRegistryInitializer::getTag,
                        aClass -> aClass));

        validateComponentName(exporterMap);

        instance.setExporters(exporterMap);
    }

    /**
     * Validate that all web component names are valid custom element names.
     *
     * @param exporterMap
     *            set of web components to validate
     */
    protected void validateComponentName(
            Map<String, Class<? extends WebComponentExporter<? extends Component>>> exporterMap) {
        for (Map.Entry<String, Class<? extends WebComponentExporter<? extends Component>>> entry : exporterMap
                .entrySet()) {
            if (!CustomElementNameValidator
                    .isCustomElementName(entry.getKey())) {
                throw new InvalidCustomElementNameException(String.format(
                        "Tag name '%s' given by '%s' is not a valid custom "
                                + "element name.",
                        entry.getKey(), entry.getValue().getCanonicalName()));
            }
        }
    }

    /**
     * Validate that we have exactly one web component exporter per tag name.
     *
     * @param exporterSet
     *            set of web components to validate
     */
    protected void validateDistinct(
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporterSet) {
        long count = exporterSet.stream()
                .map(WebComponentConfigurationRegistryInitializer::getTag)
                .distinct().count();
        if (exporterSet.size() != count) {
            Map<String, Class<?>> items = new HashMap<>();
            for (Class<? extends WebComponentExporter<? extends Component>> exporter : exporterSet) {
                String tag = getTag(exporter);
                if (items.containsKey(tag)) {
                    String message = String.format(
                            "Found two %s classes '%s' and '%s' for the tag "
                                    + "name '%s'. Tag must be unique.",
                            WebComponentExporter.class.getSimpleName(),
                            items.get(tag).getCanonicalName(),
                            exporter.getCanonicalName(), tag);
                    throw new IllegalArgumentException(message);
                }
                items.put(tag, exporter);
            }
        }
    }

    private static String getTag(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        Tag tag = exporterClass.getAnnotation(Tag.class);
        if (tag == null) {
            throw new IllegalArgumentException(String.format("%s %s did not "
                    + "provide a tag! Use %s annotation to provide a tag for "
                    + "the exported web component.",
                    WebComponentExporter.class.getSimpleName(),
                    exporterClass.getCanonicalName(),
                    Tag.class.getSimpleName()));
        }

        return tag.value();
    }
}
