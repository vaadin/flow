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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponent;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.webcomponent.WebComponentRegistry;

@HandlesTypes({ WebComponent.class })
public class WebComponentRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        WebComponentRegistry instance = WebComponentRegistry
                .getInstance(servletContext);
        if (set == null || set.isEmpty()) {
            instance.setWebComponents(Collections.emptyMap());
            return;
        }
        Set<? extends Class<? extends Component>> componentSet = set.stream()
                .filter(Component.class::isAssignableFrom)
                .filter(webComponent -> webComponent
                        .isAnnotationPresent(WebComponent.class))
                .map(clazz -> (Class<? extends Component>) clazz)
                .collect(Collectors.toSet());

        validateDistinct(componentSet);
        validateComponentName(componentSet);

        Map<String, Class<? extends Component>> webComponentMap = componentSet
                .stream()
                .collect(Collectors.toMap(this::getWebComponentName, c -> c));

        instance.setWebComponents(webComponentMap);
    }

    /**
     * Validate that all web component names are valid custom element names.
     *
     * @param componentSet
     *         set of web components to validate
     */
    protected void validateComponentName(
            Set<? extends Class<? extends Component>> componentSet) {
        for (Class<? extends Component> clazz : componentSet) {
            String tagName = getWebComponentName(clazz);
            if (!CustomElementNameValidator.isCustomElementName(tagName)) {
                String msg = String
                        .format("WebComponent name '%s' for '%s' is not a valid custom element name.",
                                tagName, clazz.getCanonicalName());
                throw new InvalidCustomElementNameException(msg);
            }
        }
    }

    /**
     * Validate that in all the components we only have one instance for each
     * element name.
     *
     * @param componentSet
     *         set of web components to validate
     */
    protected void validateDistinct(
            Set<? extends Class<? extends Component>> componentSet) {
        long count = componentSet.stream().map(this::getWebComponentName)
                .distinct().count();
        if (componentSet.size() != count) {
            Map<String, Class<?>> items = new HashMap<>();
            for (Class<? extends Component> component : componentSet) {
                String webComponentName = getWebComponentName(component);
                if (items.containsKey(webComponentName)) {
                    String message = String
                            .format("Found two WebComponents with the same name for classes '%s' and '%s'",
                                    items.get(webComponentName).getName(),
                                    component.getName());
                    throw new IllegalArgumentException(message);
                }
                items.put(webComponentName, component);
            }
        }
    }

    protected String getWebComponentName(Class<? extends Component> clazz) {
        return clazz.getAnnotation(WebComponent.class).value();
    }
}
