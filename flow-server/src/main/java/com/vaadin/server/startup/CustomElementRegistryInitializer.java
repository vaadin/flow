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
package com.vaadin.server.startup;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.server.InvalidCustomElementNameException;
import com.vaadin.ui.Component;
import com.vaadin.util.CustomElementNameValidator;

/**
 * Servlet initializer for collecting all applicable custom element tag names on
 * startup.
 */
@HandlesTypes(Tag.class)
public class CustomElementRegistryInitializer
implements ServletContainerInitializer {

    private CustomElements customElements;

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        CustomElementRegistry elementRegistry = CustomElementRegistry
                .getInstance();

        customElements = new CustomElements();
        if (classSet != null) {
            classSet.stream()
            .filter(CustomElementRegistryInitializer::isApplicableClass)
            .forEach(this::processComponentClass);
        }

        if (!elementRegistry.isInitialized()) {
            elementRegistry.setCustomElements(customElements.computeTagToElementRelation());
        }
    }

    private static boolean isApplicableClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Tag.class)
                && Component.class.isAssignableFrom(clazz)
                && PolymerTemplate.class.isAssignableFrom(clazz);
    }

    private void processComponentClass(Class<?> clazz) {
        String tagName = clazz.getAnnotation(Tag.class).value();
        if (CustomElementNameValidator.isCustomElementName(tagName)) {
            customElements.addElement(tagName, (Class<? extends Component>) clazz);
        } else {
            String msg = String.format(
                    "Tag name '%s' for '%s' is not a valid custom element name.",
                    tagName, clazz.getCanonicalName());
            throw new InvalidCustomElementNameException(msg);
        }
    }
}
