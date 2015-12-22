package com.vaadin.elements.core.grid;

import com.vaadin.elements.core.grid.event.ColumnReorderEvent;
import com.vaadin.event.EventListener;

/**
 * An event listener for column reorder events in the Grid.
 *
 * @since 7.5.0
 */
public interface ColumnReorderListener
        extends EventListener<ColumnReorderEvent> {
    /**
     * Called when the columns of the grid have been reordered.
     *
     * @param event
     *            An event providing more information
     */
    void columnReorder(ColumnReorderEvent event);
}