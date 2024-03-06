/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;pre&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.PRE)
public class Pre extends HtmlContainer implements ClickNotifier<Pre> {

    /**
     * Creates a new empty preformatted text block.
     */
    public Pre() {
        super();
    }

    /**
     * Creates a new preformatted text block with the given child components.
     *
     * @param components
     *            the child components
     */
    public Pre(Component... components) {
        super(components);
    }

    /**
     * Creates a new paragraph with the given text.
     *
     * @param text
     *            the text
     */
    public Pre(String text) {
        super();
        setText(text);
    }
}
