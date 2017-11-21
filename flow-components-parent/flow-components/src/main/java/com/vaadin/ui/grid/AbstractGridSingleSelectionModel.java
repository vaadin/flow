/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.ui.grid;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.data.selection.SelectionEvent;
import com.vaadin.data.selection.SelectionListener;
import com.vaadin.data.selection.SingleSelect;
import com.vaadin.data.selection.SingleSelectionEvent;
import com.vaadin.data.selection.SingleSelectionListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.grid.Grid.AbstractGridExtension;

import elemental.json.JsonObject;

/**
 * Abstract implementation of a GridSingleSelectionModel.
 * 
 * @author Vaadin Ltd.
 *
 * @param <T>
 *            the grid type
 */
public abstract class AbstractGridSingleSelectionModel<T>
        extends AbstractGridExtension<T>
        implements GridSingleSelectionModel<T> {

    private T selectedItem;
    private boolean deselectAllowed = true;

    /**
     * Constructor for passing a reference of the grid to this implementation.
     * 
     * @param grid
     *            reference to the grid for which this selection model is
     *            created
     */
    public AbstractGridSingleSelectionModel(Grid<T> grid) {
        super(grid);
    }

    @Override
    public void selectFromClient(T item) {
        if (Objects.equals(item, selectedItem)) {
            return;
        }
        doSelect(item, true);
    }

    @Override
    public void select(T item) {
        if (Objects.equals(item, selectedItem)) {
            return;
        }
        doSelect(item, false);
        getGrid().getDataCommunicator().reset();
    }

    @Override
    public void deselectFromClient(T item) {
        if (isSelected(item)) {
            selectFromClient(null);
        }
    }

    @Override
    public void deselect(T item) {
        if (isSelected(item)) {
            select(null);
        }
    }

    @Override
    public Optional<T> getSelectedItem() {
        return Optional.ofNullable(selectedItem);
    }

    @Override
    public void setDeselectAllowed(boolean deselectAllowed) {
        this.deselectAllowed = deselectAllowed;
    }

    @Override
    public boolean isDeselectAllowed() {
        return deselectAllowed;
    }

    @Override
    public SingleSelect<Grid<T>, T> asSingleSelect() {
        return new SingleSelect<Grid<T>, T>() {

            @Override
            public void setValue(T value) {
                setSelectedItem(value);
            }

            @Override
            public T getValue() {
                return getSelectedItem().orElse(getEmptyValue());
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Registration addValueChangeListener(
                    ValueChangeListener<Grid<T>, T> listener) {
                Objects.requireNonNull(listener, "listener cannot be null");
                return getGrid().addListener(SingleSelectionEvent.class,
                        (ComponentEventListener) listener);
            }

            @Override
            public Grid<T> get() {
                return getGrid();
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Registration addSelectionListener(SelectionListener<T> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        return getGrid().addListener(SingleSelectionEvent.class,
                (ComponentEventListener) (event -> listener
                        .selectionChange((SelectionEvent) event)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Registration addSingleSelectionListener(
            SingleSelectionListener<Grid<T>, T> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        return getGrid().addListener(SingleSelectionEvent.class,
                (ComponentEventListener) (event -> listener
                        .selectionChange((SingleSelectionEvent) event)));
    }

    @Override
    public void generateData(T item, JsonObject jsonObject) {
        if (isSelected(item)) {
            jsonObject.put("selected", true);
        }
    }

    @Override
    public void destroyData(T item) {
        deselect(item);
    }

    @Override
    public void destroyAllData() {
        deselectAll();
    }

    @Override
    protected void remove() {
        super.remove();
        deselectAll();
    }

    /**
     * Method for handling the firing of selection events.
     * 
     * @param event
     *            the selection event to fire
     */
    protected abstract void fireSelectionEvent(SelectionEvent<T> event);

    private void doSelect(T item, boolean userOriginated) {
        T oldValue = selectedItem;
        selectedItem = item;
        fireSelectionEvent(new SingleSelectionEvent<>(getGrid(),
                getGrid().asSingleSelect(), oldValue, userOriginated));
    }
}
