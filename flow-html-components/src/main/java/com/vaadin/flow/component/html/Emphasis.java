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
 * Component representing a <code>&lt;em&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.EM)
public class Emphasis extends HtmlContainer implements ClickNotifier<Emphasis> {

    /**
     * Creates a new empty emphasis.
     */
    public Emphasis() {
        super();
    }

    /**
     * Creates a new emphasis with the given child components.
     *
     * @param components
     *            the child components
     */
    public Emphasis(Component... components) {
        super(components);
    }

    /**
     * Creates a new emphasis with the given text.
     *
     * @param text
     *            the text
     */
    public Emphasis(String text) {
        super();
        setText(text);
    }
}
