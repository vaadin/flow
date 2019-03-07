/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentMethod;
import com.vaadin.flow.component.webcomponent.WebComponentProperty;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.webcomponent.WebComponentBuilder;
import com.vaadin.flow.server.webcomponent.WebComponentRegistry2;

/**
 *
 * Servlet initializer for collecting all classes that extend
 * {@link WebComponentExporter} on startup.
 *
 * @author Vaadin Ltd.
 * @since
 */
@HandlesTypes({ WebComponentExporter.class })
public class WebComponentRegistryInitializer2
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        WebComponentRegistry2 instance = WebComponentRegistry2
                .getInstance(servletContext);
        if (set == null || set.isEmpty()) {
            instance.setWebComponentBuilders(Collections.emptySet());
            return;
        }
        Set<Class<?>> exporterClasses = set.stream()
                .filter(WebComponentExporter.class::isAssignableFrom)
                .filter(c -> !c.isInterface())
                //.map(clazz -> (Class<WebComponentExporter<?extends Component>>) clazz)
                .collect(Collectors.toSet());
        Set<WebComponentExporter<? extends Component>> exporters;

        exporters = constructExporters(exporterClasses);
        validateDistinct(exporters);
        validateComponentName(exporters);

        Set<WebComponentBuilder<? extends Component>> builders =
                constructBuilders(exporters);

        //exporters.forEach(this::validateMethodsAndProperties);

        instance.setWebComponentBuilders(builders);
    }

    private Set<WebComponentBuilder<? extends Component>> constructBuilders(Set<WebComponentExporter<? extends Component>> exporters) {
        return exporters.stream().map((Function<WebComponentExporter<? extends Component>, ? extends WebComponentBuilder<? extends Component>>) WebComponentBuilder::new).collect(Collectors.toSet());
    }

    private Set<WebComponentExporter<? extends Component>> constructExporters(
            Set<Class<?>> exporterClasses) {
        Instantiator instantiator = VaadinService.getCurrent().getInstantiator();
        return exporterClasses.stream().map(instantiator::getOrCreate)
                .map(o -> (WebComponentExporter<? extends Component>)o)
                .collect(Collectors.toSet());
    }

    /**
     * Validate that all web component names are valid custom element names.
     *
     * @param exporterSet
     *         set of web components to validate
     */
    protected void validateComponentName(
            Set<WebComponentExporter<? extends Component>> exporterSet) {
        for (WebComponentExporter<? extends Component> exporter : exporterSet) {
            String tag = exporter.getTag();
            if (!CustomElementNameValidator.isCustomElementName(tag)) {
                String msg = String
                        .format("Tag name '%s' given by '%s' is not a valid " +
                                        "custom element name.",
                                tag, exporter.getClass().getCanonicalName());
                throw new InvalidCustomElementNameException(msg);
            }
        }
    }

    /**
     * Validate that we have exactly one web component exporter per tag name.
     *
     * @param exporterSet
     *         set of web components to validate
     */
    protected void validateDistinct(
            Set<WebComponentExporter<? extends Component>> exporterSet) {
        long count = exporterSet.stream().map(WebComponentExporter::getTag)
                .distinct().count();
        if (exporterSet.size() != count) {
            Map<String, Class<?>> items = new HashMap<>();
            for (WebComponentExporter<? extends Component> exporter : exporterSet) {
                String tag = exporter.getTag();
                if (items.containsKey(tag)) {
                    String message = String.format("Found two " +
                                    "WebComponentExporter classes '%s' " +
                                    "and '%s' for the tag name '%s'",
                            items.get(tag).getName(), exporter.getClass(), tag);
                    throw new IllegalArgumentException(message);
                }
                items.put(tag, exporter.getClass());
            }
        }
    }

    protected void validateMethodsAndProperties(Class<?> webComponent) {
        Set<String> methods = getWebComponentMethods(webComponent);
        Set<String> propertyFields = getWebComponentPropertyFields(
                webComponent);

        Set<String> duplicates = methods.stream()
                .filter(propertyFields::contains).collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            String message = String
                    .format("In the WebComponent '%s' there is a method and a property for the name(s) %s",
                            webComponent, duplicates);
            throw new IllegalArgumentException(message);
        }
    }

    private Set<String> getWebComponentPropertyFields(Class<?> webComponent) {
        Set<String> propertyFields = new HashSet<>();

        if (webComponent.getSuperclass() != null) {
            propertyFields.addAll(getWebComponentPropertyFields(
                    webComponent.getSuperclass()));
        }
        Stream.of(webComponent.getDeclaredFields())
                .filter(field -> WebComponentProperty.class
                        .isAssignableFrom(field.getType())).map(Field::getName)
                .forEach(propertyFields::add);

        return propertyFields;
    }

    private Set<String> getWebComponentMethods(Class<?> webComponent) {
        Set<String> methods = new HashSet<>();

        if (webComponent.getSuperclass() != null) {
            methods.addAll(
                    getWebComponentMethods(webComponent.getSuperclass()));
        }
        Stream.of(webComponent.getDeclaredMethods()).filter(method -> method
                .isAnnotationPresent(WebComponentMethod.class))
                .map(method -> method.getAnnotation(WebComponentMethod.class))
                .forEach(annotation -> methods.add(annotation.value()));

        return methods;
    }
}
