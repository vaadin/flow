/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;button&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.BUTTON)
public class NativeButton extends HtmlContainer implements
        ClickNotifier<NativeButton>, Focusable<NativeButton>, HasAriaLabel {
    /**
     * Creates a new empty button.
     */
    public NativeButton() {
        super();
    }

    /**
     * Creates a button with the given text.
     *
     * @param text
     *            the button text
     */
    public NativeButton(String text) {
        setText(text);
    }

    /**
     * Creates a button with the given text and click listener.
     *
     * @param text
     *            the button text
     * @param clickListener
     *            the click listener
     */
    public NativeButton(String text,
            ComponentEventListener<ClickEvent<NativeButton>> clickListener) {
        this(text);
        addClickListener(clickListener);
    }

}
