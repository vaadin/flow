package com.vaadin.elements.core.grid.event;

import com.vaadin.elements.core.grid.Grid;

/**
 * This event gets fired when an editor is dismissed or closed by other means.
 */
public class EditorCloseEvent extends EditorEvent {

    public EditorCloseEvent(Grid source, Object itemID) {
        super(source, itemID);
    }
}