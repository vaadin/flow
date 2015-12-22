package com.vaadin.elements.core.grid.event;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.ui.Component;

/**
 * Base class for editor related events
 */
public abstract class EditorEvent extends Component.Event {

    private Object itemID;

    protected EditorEvent(Grid source, Object itemID) {
        super(source);
        this.itemID = itemID;
    }

    /**
     * Get the item (row) for which this editor was opened
     */
    public Object getItem() {
        return itemID;
    }

}