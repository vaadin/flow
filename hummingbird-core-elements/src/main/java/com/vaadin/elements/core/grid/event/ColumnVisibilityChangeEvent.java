package com.vaadin.elements.core.grid.event;

import com.vaadin.elements.core.grid.Column;
import com.vaadin.elements.core.grid.Grid;
import com.vaadin.ui.Component;

/**
 * An event that is fired when a column's visibility changes.
 *
 * @since 7.5.0
 */
public class ColumnVisibilityChangeEvent extends Component.Event {

    private final Column column;
    private final boolean userOriginated;
    private final boolean hidden;

    /**
     * Constructor for a column visibility change event.
     *
     * @param source
     *            the grid from which this event originates
     * @param column
     *            the column that changed its visibility
     * @param hidden
     *            <code>true</code> if the column was hidden,
     *            <code>false</code> if it became visible
     * @param isUserOriginated
     *            <code>true</code> iff the event was triggered by an UI
     *            interaction
     */
    public ColumnVisibilityChangeEvent(Grid source, Column column,
            boolean hidden, boolean isUserOriginated) {
        super(source);
        this.column = column;
        this.hidden = hidden;
        userOriginated = isUserOriginated;
    }

    /**
     * Gets the column that became hidden or visible.
     *
     * @return the column that became hidden or visible.
     * @see Column#isHidden()
     */
    public Column getColumn() {
        return column;
    }

    /**
     * Was the column set hidden or visible.
     *
     * @return <code>true</code> if the column was hidden <code>false</code>
     *         if it was set visible
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Returns <code>true</code> if the column reorder was done by the user,
     * <code>false</code> if not and it was triggered by server side code.
     *
     * @return <code>true</code> if event is a result of user interaction
     */
    public boolean isUserOriginated() {
        return userOriginated;
    }
}