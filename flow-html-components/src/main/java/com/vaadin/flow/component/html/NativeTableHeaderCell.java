/*
 * Copyright (C) 2000-2026 Vaadin Ltd
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
 * Component representing a <code>&lt;th&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TH)
public class NativeTableHeaderCell extends HtmlContainer
        implements ClickNotifier<NativeTableHeaderCell> {

    /**
     * Creates a new empty header cell component.
     */
    public NativeTableHeaderCell() {
        super();
    }

    /**
     * Creates a new header cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    public NativeTableHeaderCell(Component... components) {
        super(components);
    }

    /**
     * Creates a new header cell with the given text.
     *
     * @param text
     *            the text.
     */
    public NativeTableHeaderCell(String text) {
        super();
        setText(text);
    }
}
