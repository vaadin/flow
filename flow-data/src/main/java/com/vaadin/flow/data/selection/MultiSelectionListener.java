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
 * A listener for listening for selection changes from a multiselection
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
 * @see SelectionModel.Multi
 * @see MultiSelectionEvent
 */
@FunctionalInterface
public interface MultiSelectionListener<C extends Component, T>
        extends Serializable, EventListener {

    /**
     * Invoked when the selection has changed by the user or programmatically.
     *
     * @param event
     *            the selection event, never {@code null}
     */
    void selectionChange(MultiSelectionEvent<C, T> event);
}
