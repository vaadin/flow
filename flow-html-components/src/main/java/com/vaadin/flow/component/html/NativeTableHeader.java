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
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;thead&gt;</code> element.
 *
 * @since 24.5
 */
@Tag(Tag.THEAD)
public class NativeTableHeader extends HtmlContainer
        implements NativeTableRowContainer, ClickNotifier<NativeTableHeader> {

    /**
     * Creates a new empty table header component.
     */
    public NativeTableHeader() {
        super();
    }

    /**
     * Creates a new table header with the given children components.
     *
     * @param components
     *            the children components.
     */
    public NativeTableHeader(Component... components) {
        super(components);
    }
}
