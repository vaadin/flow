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

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.vaadin.hummingbird.dom.Element;

/**
 * A Component is a higher level abstraction of an {@link Element} or a
 * hierarchy of {@link Element}s.
 * <p>
 * A Component must attach itself to its element, as returned by
 * {@link #getElement()}, once the element has been decided. The element must be
 * decided in the constructor and the element must not change during the
 * lifetime of the component.
 *
 * @author Vaadin
 * @since
 */
public interface Component extends Serializable {
    /**
     * Gets the root element of this component.
     * <p>
     * Each component must have exactly one root element. This element is
     * attached to the {@link Element} tree when this component is attached to a
     * parent component.
     *
     * @return the root element of this component.
     */
    Element getElement();

    /**
     * Gets the parent component of this component.
     * <p>
     * A component can only have one parent.
     *
     * @return the parent component
     */
    default Optional<Component> getParent() {
        assert ComponentUtil.isAttachedTo(this, getElement());

        Element parentElement = getElement().getParent();
        while (parentElement != null
                && !parentElement.getComponent().isPresent()) {
            parentElement = parentElement.getParent();
        }

        if (parentElement == null) {
            return Optional.empty();
        }

        return parentElement.getComponent();
    }

    /**
     * Gets the child components of this component.
     * <p>
     * The default implementation finds child components by traversing each
     * child {@link Element} tree.
     *
     * @return the child components of this component
     */
    default Stream<Component> getChildren() {
        assert ComponentUtil.isAttachedTo(this, getElement());

        Builder<Component> childComponents = Stream.builder();
        getElement().getChildren().forEach(childElement -> {
            ComponentUtil.findComponents(childElement, component -> {
                childComponents.add(component);
            });
        });
        return childComponents.build();
    }

}
