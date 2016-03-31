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
import java.util.stream.Stream.Builder;

import com.vaadin.hummingbird.dom.Element;

/**
 * Utility methods for {@link Component}.
 *
 * @author Vaadin
 * @since
 */
public interface ComponentUtil {

    /**
     * Finds the first component instance in each {@link Element} subtree by
     * traversing the {@link Element} tree starting from the given element.
     *
     * @param element
     *            the element to start scanning from
     * @param components
     *            the builder c
     */
    static void findComponents(Element element, Builder<Component> components) {
        assert element != null;
        assert components != null;

        if (element.getComponent().isPresent()) {
            components.add(element.getComponent().get());
            return;
        }

        for (int i = 0; i < element.getChildCount(); i++) {
            Element childElement = element.getChild(i);
            findComponents(childElement, components);
        }
    }

    /**
     * Checks if the given component is attached to the given element.
     *
     * @param component
     *            the component
     * @param element
     *            the element
     * @return {@code true} if the component is attached to the given element,
     *         {@code false} otherwise
     */
    static boolean isAttachedTo(Component component, Element element) {
        Optional<Component> elementComponent = element.getComponent();
        return elementComponent.isPresent()
                && elementComponent.get() == component;
    }
}
