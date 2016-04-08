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
import com.vaadin.hummingbird.dom.ElementUtil;

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

        Optional<Component> maybeComponent = ElementUtil.getComponent(element);

        if (maybeComponent.isPresent()) {
            Component component = maybeComponent.get();
            componentConsumer.accept(component);
            return;
        }

        for (int i = 0; i < element.getChildCount(); i++) {
            Element childElement = element.getChild(i);
            findComponents(childElement, componentConsumer);
        }
    }

    /**
     * Gets the parent of the given component, which is inside the given
     * composite.
     * <p>
     * This method is meant for internal use only.
     *
     * @param composite
     *            the composite containing the component
     * @param component
     *            the component to get the parent for, must be inside the
     *            composite or a nested composite
     * @return the parent of the component, never <code>null</code>
     */
    static Component getParentUsingComposite(Composite composite,
            Component component) {
        // If this is the component inside a composite or a nested
        // composite, we need to traverse the composite hierarchy to find
        // the parent
        Composite compositeAncestor = composite;
        while (true) {
            Component compositeChild = compositeAncestor.getContent();
            if (compositeChild == component) {
                return compositeAncestor;
            }
            compositeAncestor = (Composite) compositeAncestor.getContent();
        }

    }

    /**
     * Returns the innermost component from a {@link Composite} chain, i.e. the
     * first content which is not a {@link Composite}.
     *
     * @param composite
     *            a composite in the chain
     * @return the innermost component
     */
    static Component getInnermostComponent(Composite composite) {
        Component content = composite.getContent();
        while (content instanceof Composite) {
            content = ((Composite) content).getContent();
        }
        return content;
    }

    /**
     * Checks if the given component is inside a {@link Composite} chain, i.e.
     * it is a composite in the chain or the content of the innermost
     * {@link Composite}.
     *
     * @param composite
     *            the first composite
     * @param component
     *            the component to look for
     * @return <code>true</code> if the component is inside the composite chain,
     *         <code>false</code> otherwise
     */
    static boolean isCompositeContent(Composite composite,
            Component component) {
        Component compositeContent = composite.getContent();
        if (compositeContent == component) {
            return true;
        } else if (compositeContent instanceof Composite) {
            // Nested composites
            return isCompositeContent((Composite) compositeContent, component);
        } else {
            return false;
        }
    }

    /**
     * Finds the first component by traversing upwards in the element hierarchy,
     * starting from the given element.
     *
     * @param element
     * @return
     */
    static Optional<Component> findParentComponent(Element element) {
        Element mappedElement = element;
        while (mappedElement != null
                && !ElementUtil.getComponent(mappedElement).isPresent()) {
            mappedElement = mappedElement.getParent();
        }
        if (mappedElement == null) {
            return Optional.empty();
        }

        return Optional.of(getInnermostComponent(mappedElement));
    }

    /**
     * Gets the innermost mapped component for the element.
     * <p>
     * This returns {@link ElementUtil#getComponent(Element)} if something else
     * than a {@link Composite} is mapped to the element. If a {@link Composite}
     * is mapped to the element, finds the innermost content of the
     * {@link Composite} chain.
     *
     * @param element
     *            the element which is mapped to a component
     * @return the innermost component mapped to the element
     */
    static Component getInnermostComponent(Element element) {
        assert ElementUtil.getComponent(element).isPresent();

        Component component = ElementUtil.getComponent(element).get();
        if (component instanceof Composite) {
            return ComponentUtil.getInnermostComponent((Composite) component);
        } else {
            return component;
        }
    }

}
