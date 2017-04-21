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
import com.vaadin.util.CustomElementNameValidator;

/**
 * Flow servlet initializer for collecting all applicable custom element tag
 * names on startup.
 */
@HandlesTypes(Tag.class)
public class FlowCustomElements implements ServletContainerInitializer {

    public static final Map<String, Class<?>> customElements = new HashMap<>();

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        set.forEach(clazz -> processComponentClass(clazz));
    }

    private void processComponentClass(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Tag.class)) {
            String tagName = clazz.getAnnotation(Tag.class).value();
            if (customElements.containsKey(tagName)) {
                Class<?> componentClass = customElements.get(tagName);
                // Test if componentClass is superclass of clazz
                if (clazz.isAssignableFrom(componentClass)) {
                    // Replace existing sub class with super class
                    customElements.put(tagName, clazz);
                } else if(!componentClass.isAssignableFrom(clazz)) {
                    // Throw exception if neither class is a super class of the other.
                    throw new ClassCastException("Incompatible components for tag name: " + tagName);
                }
            } else if (CustomElementNameValidator
                    .isValidCustomElementName(tagName)) {
                customElements.put(tagName, clazz);
            }
        }
    }
}
