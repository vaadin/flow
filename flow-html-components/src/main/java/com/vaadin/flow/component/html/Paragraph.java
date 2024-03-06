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
 * Component representing a <code>&lt;p&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.P)
public class Paragraph extends HtmlContainer
        implements ClickNotifier<Paragraph> {

    /**
     * Creates a new empty paragraph.
     */
    public Paragraph() {
        super();
    }

    /**
     * Creates a new paragraph with the given child components.
     *
     * @param components
     *            the child components
     */
    public Paragraph(Component... components) {
        super(components);
    }

    /**
     * Creates a new paragraph with the given text.
     *
     * @param text
     *            the text
     */
    public Paragraph(String text) {
        super();
        setText(text);
    }
}
