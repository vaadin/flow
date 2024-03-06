/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

}
