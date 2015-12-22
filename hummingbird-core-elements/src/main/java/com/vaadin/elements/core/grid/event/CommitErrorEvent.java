package com.vaadin.elements.core.grid.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.elements.core.grid.Column;
import com.vaadin.elements.core.grid.Grid;
import com.vaadin.ui.Component;

/**
 * An event which is fired when saving the editor fails
 */
public class CommitErrorEvent extends Component.Event {

    private CommitException cause;

    private Set<Column> errorColumns = new HashSet<Column>();

    private String userErrorMessage;

    public CommitErrorEvent(Grid grid, CommitException cause) {
        super(grid);
        this.cause = cause;
        userErrorMessage = cause.getLocalizedMessage();
    }

    /**
     * Retrieves the cause of the failure
     *
     * @return the cause of the failure
     */
    public CommitException getCause() {
        return cause;
    }

    @Override
    public Grid getComponent() {
        return (Grid) super.getComponent();
    }

    /**
     * Checks if validation exceptions caused this error
     *
     * @return true if the problem was caused by a validation error
     */
    public boolean isValidationFailure() {
        return cause.getCause() instanceof InvalidValueException;
    }

    /**
     * Marks that an error indicator should be shown for the editor of a
     * column.
     *
     * @param column
     *            the column to show an error for
     */
    public void addErrorColumn(Column column) {
        errorColumns.add(column);
    }

    /**
     * Gets all the columns that have been marked as erroneous.
     *
     * @return an umodifiable collection of erroneous columns
     */
    public Collection<Column> getErrorColumns() {
        return Collections.unmodifiableCollection(errorColumns);
    }

    /**
     * Gets the error message to show to the user.
     *
     * @return error message to show
     */
    public String getUserErrorMessage() {
        return userErrorMessage;
    }

    /**
     * Sets the error message to show to the user.
     *
     * @param userErrorMessage
     *            the user error message to set
     */
    public void setUserErrorMessage(String userErrorMessage) {
        this.userErrorMessage = userErrorMessage;
    }

}