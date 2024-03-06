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
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;div&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.DIV)
public class Div extends HtmlContainer
        implements ClickNotifier<Div>, HasOrderedComponents {

    /**
     * Creates a new empty div.
     */
    public Div() {
        super();
    }

    /**
     * Creates a new div with the given child components.
     *
     * @param components
     *            the child components
     */
    public Div(Component... components) {
        super(components);
    }
}
