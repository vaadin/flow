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
import java.util.function.Consumer;

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
     * @param componentConsumer
     *            a consumer which is called for each found component
     */
    static void findComponents(Element element,
            Consumer<Component> componentConsumer) {
        assert element != null;
        assert componentConsumer != null;

        Optional<Component> component = element.getComponent();

        if (component.isPresent()) {
            Optional<Composite> composite = element.getComposite();
            if (composite.isPresent()) {
                // If element has one or more composites, return the outermost
                componentConsumer.accept(composite.get());
                return;
            } else {
                componentConsumer.accept(element.getComponent().get());
                return;
            }
        }

        for (int i = 0; i < element.getChildCount(); i++) {
            Element childElement = element.getChild(i);
            findComponents(childElement, componentConsumer);
        }
    }

}
