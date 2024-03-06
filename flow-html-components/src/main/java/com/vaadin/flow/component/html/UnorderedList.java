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
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;ul&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.UL)
public class UnorderedList extends HtmlContainer
        implements ClickNotifier<UnorderedList> {

    /**
     * Creates a new empty unordered list.
     */
    public UnorderedList() {
        super();
    }

    /**
     * Creates a new unordered list with the given list items.
     *
     * @param items
     *            the list items
     */
    public UnorderedList(ListItem... items) {
        super(items);
    }
}
