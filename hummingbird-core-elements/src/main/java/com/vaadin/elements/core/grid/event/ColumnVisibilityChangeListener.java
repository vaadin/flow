package com.vaadin.elements.core.grid.event;

import com.vaadin.event.EventListener;

/**
 * An event listener for column visibility change events in the Grid.
 *
 * @since 7.5.0
 */
public interface ColumnVisibilityChangeListener
        extends EventListener<ColumnVisibilityChangeEvent> {
    /**
     * Called when a column has become hidden or unhidden.
     *
     * @param event
     */
    void columnVisibilityChanged(ColumnVisibilityChangeEvent event);
}