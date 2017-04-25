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
package com.vaadin.server;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.ui.Component;
import com.vaadin.util.CustomElementNameValidator;

/**
 * Flow servlet initializer for collecting all applicable custom element tag
 * names on startup.
 */
@HandlesTypes(Tag.class)
public class CustomElementRegistryInitializer
        implements ServletContainerInitializer {

    private Map<String, Class<? extends Component>> customElements;

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        CustomElementRegistry elementRegistry = CustomElementRegistry.getInstance();

        if(!elementRegistry.initialized) {
            return;
        }

        customElements = new HashMap<>();
        set.forEach(this::processComponentClass);

        elementRegistry.setCustomElements(customElements);
    }

    private void processComponentClass(Class<?> clazz) {
        if (isApplicableClass(clazz)) {
            String tagName = clazz.getAnnotation(Tag.class).value();
            if (customElements.containsKey(tagName)) {
                updateRegisteredClassIfNecessary(tagName, clazz);
            } else {
                CustomElementNameValidator.Result result = CustomElementNameValidator
                        .validate(tagName);
                if (result.isValid()) {
                    customElements.put(tagName,
                            (Class<? extends Component>) clazz);
                } else {
                    String msg = String.format(
                            "Tag name '%s' for '%s' is not a valid custom element name.",
                            tagName, clazz.getSimpleName());
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    private boolean isApplicableClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Tag.class)
                && Component.class.isAssignableFrom(clazz)
                && PolymerTemplate.class.isAssignableFrom(clazz);
    }

    /**
     * If we have a conflict with multiple classes with the same tag name we
     * should register the superclass of the two.
     *
     * @param tagName
     *            tag name of new and existing class
     * @param newClass
     *            new class that we try to register
     * @throws ClassCastException
     *             thrown if neither class is a super class of the other
     */
    private void updateRegisteredClassIfNecessary(String tagName,
            Class<?> newClass) throws ClassCastException {

        Class<? extends Component> componentClass = customElements.get(tagName);

        // Test if componentClass is superclass of newClass
        if (newClass.isAssignableFrom(componentClass)) {
            // Replace existing sub class with super class
            customElements.put(tagName, (Class<? extends Component>) newClass);
        } else if (!componentClass.isAssignableFrom(newClass)) {
            String msg = String.format(
                    "Incompatible tag '%s' annotation for components '%s' and '%s'",
                    tagName, componentClass.getSimpleName(),
                    newClass.getSimpleName());
            // Throw exception if neither class is a super class of the
            // other.
            throw new ClassCastException(msg);
        }
    }
}
