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
package com.vaadin.flow.server.startup;

import java.util.Map;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;

/**
 * Common validation methods for custom element registry initializer.
 *
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractCustomElementRegistryInitializer {

    /**
     * Filter custom element classes from the {@code classes} stream.
     *
     * @param classes
     *            potential component classes
     * @return a resulting map of the custom component classes
     */
    protected Map<String, Class<? extends Component>> filterCustomElements(
            Stream<Class<?>> classes) {
        CustomElements customElements = new CustomElements();
        classes.filter(this::isApplicableClass)
                .forEach(clazz -> processComponentClass(clazz, customElements));
        return customElements.computeTagToElementRelation();
    }

    private boolean isApplicableClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Tag.class)
                && Component.class.isAssignableFrom(clazz)
                && PolymerTemplate.class.isAssignableFrom(clazz);
    }

    private void processComponentClass(Class<?> clazz,
            CustomElements customElements) {
        String tagName = clazz.getAnnotation(Tag.class).value();
        if (CustomElementNameValidator.isCustomElementName(tagName)) {
            customElements.addElement(tagName,
                    (Class<? extends Component>) clazz);
        } else {
            String msg = String.format(
                    "Tag name '%s' for '%s' is not a valid custom element name.",
                    tagName, clazz.getCanonicalName());
            throw new InvalidCustomElementNameException(msg);
        }
    }
}
