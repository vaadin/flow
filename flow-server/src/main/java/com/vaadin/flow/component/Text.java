/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;

/**
 * A component which encapsulates the given text in a text node.
 * <p>
 * Text node doesn't support setting any attribute or property so you may not
 * use Element API (and {@link Text} doesn't provide any such contract) for
 * setting attribute/property. It implies that you may not style this component
 * as well. Any attempt to set attribute/property value throws an exception. The
 * only available API for a {@link Text} component is set a text.
 * <p>
 * If you need a text component which can be styled then check {@code Span}
 * class (from {@code flow-html-components}) module.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Text extends Component implements HasText {

    /**
     * Creates an instance using the given text.
     *
     * @param text
     *            the text to show, <code>null</code> is interpreted as an empty
     *            string
     */
    public Text(String text) {
        super(Element.createText(text));
    }

    /**
     * Sets the text of the component.
     *
     * @param text
     *            the text of the component, <code>null</code> is interpreted as
     *            an empty string
     */
    @Override
    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        getElement().setText(text);
    }

    /**
     * Gets the text of the component.
     *
     * @return the text of the component, not <code>null</code>
     */
    @Override
    public String getText() {
        return getElement().getText();
    }

    @Override
    protected <T> void set(PropertyDescriptor<T, ?> descriptor, T value) {
        throw new UnsupportedOperationException("Cannot set '"
                + descriptor.getPropertyName() + "' property to the "
                + getClass().getSimpleName() + " component because it doesn't "
                + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void setId(String id) {
        super.setId(id);
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void setVisible(boolean visible) {
        throw new UnsupportedOperationException("Cannot change "
                + getClass().getSimpleName()
                + " component visibility because it doesn't "
                + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void addClassName(String className) {
        throw new UnsupportedOperationException("Cannot add a class to the "
                + getClass().getSimpleName() + " component because it doesn't "
                + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean removeClassName(String className) {
        throw new UnsupportedOperationException(
                "Cannot remove a class from the " + getClass().getSimpleName()
                        + " component because it doesn't "
                        + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void setClassName(String className) {
        throw new UnsupportedOperationException("Cannot set the "
                + getClass().getSimpleName()
                + " component class because it doesn't "
                + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void setClassName(String className, boolean set) {
        throw new UnsupportedOperationException("Cannot set the "
                + getClass().getSimpleName()
                + " component class because it doesn't "
                + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void addClassNames(String... classNames) {
        throw new UnsupportedOperationException("Cannot add classes to the "
                + getClass().getSimpleName() + " component because it doesn't "
                + "represent an HTML Element but a text Node on the client side.");
    }

    /**
     * The method is not supported for the {@link Text} class.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void removeClassNames(String... classNames) {
        throw new UnsupportedOperationException(
                "Cannot remove classes from the " + getClass().getSimpleName()
                        + " component because it doesn't "
                        + "represent an HTML Element but a text Node on the client side.");
    }
}
