package com.vaadin.elements.core.grid.event;

import com.vaadin.elements.core.grid.Grid;

/**
 * This event gets fired when an editor is opened
 */
public class EditorOpenEvent extends EditorEvent {

    public EditorOpenEvent(Grid source, Object itemID) {
        super(source, itemID);
    }
}