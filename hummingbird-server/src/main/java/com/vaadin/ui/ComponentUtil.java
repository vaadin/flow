/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.util.Optional;

import com.vaadin.hummingbird.dom.Element;

/**
 * Provides utility methods for {@link Component}.
 *
 * @author Vaadin
 * @since
 */
public interface ComponentUtil {

    /**
     * Finds the first component instance by scanning the {@link Element} tree
     * starting from the given element.
     *
     * @param element
     *            the element to start scanning from
     * @return this first component, if found
     */
    static Optional<Component> findFirstComponent(Element element) {
        assert element != null;

        if (element.getComponent().isPresent()) {
            return element.getComponent();
        }

        for (int i = 0; i < element.getChildCount(); i++) {
            Element childElement = element.getChild(i);
            Optional<Component> component = findFirstComponent(childElement);
            if (component.isPresent()) {
                return component;
            }
        }

        return Optional.empty();
    }
}
