package com.vaadin.elements.core.grid.event;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.ui.Component;

/**
 * An event that is fired when the columns are reordered.
 *
 * @since 7.5.0
 */
public class ColumnReorderEvent extends Component.Event {

    /**
     * Is the column reorder related to this event initiated by the user
     */
    private final boolean userOriginated;

    /**
     *
     * @param source
     *            the grid where the event originated from
     * @param userOriginated
     *            <code>true</code> if event is a result of user interaction,
     *            <code>false</code> if from API call
     */
    public ColumnReorderEvent(Grid source, boolean userOriginated) {
        super(source);
        this.userOriginated = userOriginated;
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