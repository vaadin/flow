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
 * A listener for listening to selection changes on a single selection
 * component.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <C>
 *            the selection component type
 * @param <T>
 *            the type of the selected item
 *
 * @see SelectionModel.Single
 * @see SingleSelectionEvent
 */
@FunctionalInterface
public interface SingleSelectionListener<C extends Component, T>
        extends Serializable, EventListener {

    /**
     * Invoked when selection has been changed.
     *
     * @param event
     *            the single selection event
     */
    void selectionChange(SingleSelectionEvent<C, T> event);
}
