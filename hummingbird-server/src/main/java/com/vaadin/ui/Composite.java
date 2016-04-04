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

import java.util.stream.Stream;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementUtil;

/**
 * A composite encapsulates a {@link Component} tree to allow creation of new
 * components by composing existing components. By encapsulating the component,
 * its API can be hidden or presented in a different way for the user of the
 * composite.
 * <p>
 * {@link Composite} must always be extended and the encapsulated component tree
 * specified through the overridden {@link #initContent()} method. The
 * encapsulated component itself can contain more components.
 * <p>
 * Composite is a way to hide API on the server side. It does not contribute any
 * element to the {@link Element} tree.
 *
 * @author Vaadin
 * @since
 */
public abstract class Composite extends Component {

    private Component content;

    /**
     * Creates a new composite.
     * <p>
     * To define your own composite, extend this class and implement
     * {@link #initContent()}.
     */
    protected Composite() {
        super(null);
    }

    /**
     * Called when the content of this composite is requested for the first
     * time.
     * <p>
     * This method should initialize the component structure for the composite
     * and return the root component.
     *
     * @return the root component which this composite wraps, never {@code null}
     */
    protected abstract Component initContent();

    /**
     * Gets the content of the composite, i.e. the component the composite is
     * wrapping.
     *
     * @return the content for the composite, never {@code null}
     */
    protected Component getContent() {
        if (content == null) {
            Component newContent = initContent();
            if (newContent == null) {
                throw new IllegalStateException(
                        "initContent returned null instead of a component");
            }
            setContent(newContent);
        }
        return content;
    }

    /**
     * Sets the content for this composite and attaches it to the element.
     * <p>
     * This method must only be called once.
     *
     * @param content
     *            the content for the composite
     */
    private void setContent(Component content) {
        assert ElementUtil.getComponent(content.getElement())
                .isPresent() : "Composite should never be attached to an element which is not attached to a component";
        assert this.content == null : "Content has already been initialized";
        this.content = content;
        Element element = content.getElement();
        // Always replace the composite reference as this will be called from
        // inside out, so the end result is that the element refers to the
        // outermost composite in the probably rare case that multiple
        // composites are nested
        ElementUtil.setComponent(element, this);
    }

    @Override
    public Element getElement() {
        // For composite, the element field in the super class is always null
        return getContent().getElement();
    }

    @Override
    public Stream<Component> getChildren() {
        return Stream.of(getContent());
    }
}
