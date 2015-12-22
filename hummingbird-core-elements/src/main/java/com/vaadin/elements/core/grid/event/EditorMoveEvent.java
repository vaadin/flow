package com.vaadin.elements.core.grid.event;

import com.vaadin.elements.core.grid.Grid;

/**
 * This event gets fired when an editor is opened while another row is being
 * edited (i.e. editor focus moves elsewhere)
 */
public class EditorMoveEvent extends EditorEvent {

    public EditorMoveEvent(Grid source, Object itemID) {
        super(source, itemID);
    }
}