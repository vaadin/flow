/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.selection;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.flow.component.Component;

/**
 * A listener for {@code SelectionEvent}.
 * <p>
 * This is a generic listener for both type of selections, single and
 * multiselect.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the type of the selected item
 * @param <C>
 *            the component type
 *
 * @see SelectionEvent
 */
@FunctionalInterface
public interface SelectionListener<C extends Component, T>
        extends Serializable, EventListener {

    /**
     * Invoked when the selection has changed.
     *
     * @param event
     *            the selection event
     */
    void selectionChange(SelectionEvent<C, T> event);
}
