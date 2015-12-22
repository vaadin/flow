package com.vaadin.elements.core.grid;

import com.vaadin.data.fieldgroup.FieldGroup;

/**
 * Custom field group that allows finding property types before an item has been
 * bound.
 */
final class CustomFieldGroup extends FieldGroup {

    /**
     *
     */
    private final Grid grid;

    public CustomFieldGroup(Grid grid) {
        this.grid = grid;
        // setFieldFactory(EditorFieldFactory.get());
    }

    @Override
    protected Class<?> getPropertyType(Object propertyId) throws BindException {
        if (getItemDataSource() == null) {
            return grid.getContainerDataSource().getType(propertyId);
        } else {
            return super.getPropertyType(propertyId);
        }
    }
}