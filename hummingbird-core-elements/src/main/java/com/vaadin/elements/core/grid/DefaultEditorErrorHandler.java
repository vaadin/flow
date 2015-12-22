package com.vaadin.elements.core.grid;

import java.util.Map;
import java.util.Set;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.elements.core.grid.event.CommitErrorEvent;
import com.vaadin.elements.core.grid.event.EditorErrorHandler;
import com.vaadin.server.ClientConnector.ConnectorErrorEvent;
import com.vaadin.ui.Field;

/**
 * Default error handler for the editor
 *
 */
public class DefaultEditorErrorHandler implements EditorErrorHandler {

    /**
     * 
     */
    private final Grid grid;

    /**
     * @param grid
     */
    DefaultEditorErrorHandler(Grid grid) {
        this.grid = grid;
    }

    @Override
    public void commitError(CommitErrorEvent event) {
        Map<Field<?>, InvalidValueException> invalidFields = event
                .getCause().getInvalidFields();

        if (!invalidFields.isEmpty()) {
            Object firstErrorPropertyId = null;
            Field<?> firstErrorField = null;

            FieldGroup fieldGroup = event.getCause().getFieldGroup();
            for (Column column : this.grid.getColumns()) {
                Object propertyId = column.getPropertyId();
                Field<?> field = fieldGroup.getField(propertyId);
                if (invalidFields.keySet().contains(field)) {
                    event.addErrorColumn(column);

                    if (firstErrorPropertyId == null) {
                        firstErrorPropertyId = propertyId;
                        firstErrorField = field;
                    }
                }
            }

            /*
             * Validation error, show first failure as
             * "<Column header>: <message>"
             */
            String caption = this.grid.getColumn(firstErrorPropertyId)
                    .getHeaderCaption();
            String message = invalidFields.get(firstErrorField)
                    .getLocalizedMessage();

            event.setUserErrorMessage(caption + ": " + message);
        } else {
            com.vaadin.server.ErrorEvent.findErrorHandler(this.grid).error(
                    new ConnectorErrorEvent(this.grid, event.getCause()));
        }
    }

    private Object getFirstPropertyId(FieldGroup fieldGroup,
            Set<Field<?>> keySet) {
        for (Column c : this.grid.getColumns()) {
            Object propertyId = c.getPropertyId();
            Field<?> f = fieldGroup.getField(propertyId);
            if (keySet.contains(f)) {
                return propertyId;
            }
        }
        return null;
    }
}