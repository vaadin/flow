package com.vaadin.ui.grid;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.data.provider.DataGenerator;
import com.vaadin.data.selection.SelectionEvent;
import com.vaadin.data.selection.SelectionListener;
import com.vaadin.data.selection.SingleSelect;
import com.vaadin.data.selection.SingleSelectionEvent;
import com.vaadin.data.selection.SingleSelectionListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.grid.Grid.AbstractGridExtension;

import elemental.json.JsonObject;

public abstract class AbstractGridSingleSelectionModel<T>
        extends AbstractGridExtension<T>
        implements GridSingleSelectionModel<T>, DataGenerator<T> {

    private T selectedItem = null;
    private boolean deselectAllowed = true;

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
        fireSelectionEvent(new SingleSelectionEvent<Grid<T>, T>(getGrid(),
                getGrid().asSingleSelect(), oldValue, userOriginated));
    }
}
