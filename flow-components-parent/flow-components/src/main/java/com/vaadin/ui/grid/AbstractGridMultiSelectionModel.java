package com.vaadin.ui.grid;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.provider.Query;
import com.vaadin.data.selection.MultiSelect;
import com.vaadin.data.selection.MultiSelectionEvent;
import com.vaadin.data.selection.MultiSelectionListener;
import com.vaadin.data.selection.SelectionEvent;
import com.vaadin.data.selection.SelectionListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.ComponentEventListener;

/**
 * Abstract implementation of a GridMultiSelectionModel.
 * 
 * @author Vaadin Ltd.
 *
 * @param <T>
 *            the grid type
 */
public abstract class AbstractGridMultiSelectionModel<T>
        implements GridMultiSelectionModel<T> {


    private final Set<T> selected;
    private final GridSelectionColumn selectionColumn;
    private final Grid<T> grid;
    private SelectAllCheckboxVisibility selectAllCheckBoxVisibility;
    
    /**
     * Constructor for passing a reference of the grid to this implementation.
     * 
     * @param grid
     *            reference to the grid for which this selection model is
     *            created
     */
    public AbstractGridMultiSelectionModel(Grid<T> grid) {
        this.grid = grid;
        selected = new LinkedHashSet<>();
        selectionColumn = new GridSelectionColumn(this::clientSelectAll,
                this::clientDeselectAll);
        selectAllCheckBoxVisibility = SelectAllCheckboxVisibility.DEFAULT;

        selectionColumn
                .setSelectAllCheckBoxVisibility(isSelectAllCheckboxVisible());
        grid.getElement().getNode().runWhenAttached(ui -> grid.getElement()
                .insertChild(0, selectionColumn.getElement()));
    }

    @Override
    public void remove() {
        deselectAll();
        if (selectionColumn.getElement().getNode().isAttached()) {
            grid.getElement().removeChild(selectionColumn.getElement());
        }
    }

    @Override
    public void selectFromClient(T item) {
        doSelect(item, true);
    }

    @Override
    public void deselectFromClient(T item) {
        doDeselect(item, true);
    }

    @Override
    public Set<T> getSelectedItems() {
        return Collections.unmodifiableSet(selected);
    }

    @Override
    public Optional<T> getFirstSelectedItem() {
        return selected.stream().findFirst();
    }

    @Override
    public void select(T item) {
        doSelect(item, false);
        grid.getDataCommunicator().reset();
    }

    @Override
    public void deselect(T item) {
        doDeselect(item, false);
        grid.getDataCommunicator().reset();
        selectionColumn.setSelectAllCheckboxState(false);
    }

    @Override
    public void selectAll() {
        updateSelection(
                grid.getDataCommunicator().getDataProvider()
                        .fetch(new Query<>())
                        .collect(Collectors.toSet()),
                Collections.emptySet());
        selectionColumn.setSelectAllCheckboxState(true);
    }

    @Override
    public void deselectAll() {
        updateSelection(Collections.emptySet(),
                getSelectedItems());
        selectionColumn.setSelectAllCheckboxState(false);
    }

    @Override
    public void updateSelection(Set<T> addedItems,
            Set<T> removedItems) {
        Objects.requireNonNull(addedItems,
                "added items cannot be null");
        Objects.requireNonNull(removedItems,
                "removed items cannot be null");
        doUpdateSelection(addedItems, removedItems, false);
    }

    @Override
    public boolean isSelected(T item) {
        return getSelectedItems().contains(item);
    }

    @Override
    public MultiSelect<Grid<T>, T> asMultiSelect() {
        return new MultiSelect<Grid<T>, T>() {

            @Override
            public void setValue(Set<T> value) {
                Objects.requireNonNull(value);
                Set<T> copy = value.stream()
                        .map(Objects::requireNonNull)
                        .collect(Collectors.toCollection(
                                LinkedHashSet::new));
                updateSelection(copy, new LinkedHashSet<>(
                        getSelectedItems()));
            }

            @Override
            public Set<T> getValue() {
                return getSelectedItems();
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Registration addValueChangeListener(
                    ValueChangeListener<Grid<T>, Set<T>> listener) {
                Objects.requireNonNull(listener,
                        "listener cannot be null");
                return grid.addListener(
                        MultiSelectionEvent.class,
                        (ComponentEventListener) listener);
            }

            @Override
            public Grid<T> get() {
                return grid;
            }

            @Override
            public Set<T> getEmptyValue() {
                return Collections.emptySet();
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Registration addSelectionListener(
            SelectionListener<T> listener) {
        Objects.requireNonNull(listener,
                "listener cannot be null");
        return grid.addListener(MultiSelectionEvent.class,
                (ComponentEventListener) (event -> listener
                        .selectionChange(
                                (SelectionEvent) event)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Registration addMultiSelectionListener(
            MultiSelectionListener<Grid<T>, T> listener) {
        Objects.requireNonNull(listener,
                "listener cannot be null");
        return grid.addListener(MultiSelectionEvent.class,
                (ComponentEventListener) (event -> listener
                        .selectionChange(
                                (MultiSelectionEvent) event)));
    }

    @Override
    public void setSelectAllCheckboxVisibility(
            SelectAllCheckboxVisibility selectAllCheckBoxVisibility) {
        this.selectAllCheckBoxVisibility = selectAllCheckBoxVisibility;
        selectionColumn.setSelectAllCheckBoxVisibility(
                isSelectAllCheckboxVisible());
    }

    @Override
    public SelectAllCheckboxVisibility getSelectAllCheckboxVisibility() {
        return selectAllCheckBoxVisibility;
    }

    @Override
    public boolean isSelectAllCheckboxVisible() {
        switch (selectAllCheckBoxVisibility) {
        case DEFAULT:
            return grid.getDataCommunicator().getDataProvider()
                    .isInMemory();
        case HIDDEN:
            return false;
        case VISIBLE:
            return true;
        default:
            throw new IllegalStateException(String.format(
                    "Select all checkbox visibility is set to an unsupported value: %s",
                    selectAllCheckBoxVisibility));
        }
    }

    /**
     * Method for handling the firing of selection events.
     * 
     * @param event
     *            the selection event to fire
     */
    protected abstract void fireSelectionEvent(SelectionEvent<T> event);

    private void clientSelectAll() {
        doUpdateSelection(
                grid.getDataCommunicator().getDataProvider()
                        .fetch(new Query<>()).collect(Collectors.toSet()),
                Collections.emptySet(), true);
        selectionColumn.setSelectAllCheckboxState(true);
    }

    private void clientDeselectAll() {
        doUpdateSelection(Collections.emptySet(), getSelectedItems(), true);
        selectionColumn.setSelectAllCheckboxState(false);
    }

    private void doSelect(T item, boolean userOriginated) {
        Set<T> oldSelection = new LinkedHashSet<>(selected);
        boolean added = selected.add(item);
        if (added) {
            fireSelectionEvent(new MultiSelectionEvent<Grid<T>, T>(
                    grid, grid.asMultiSelect(), oldSelection,
                    userOriginated));
        }
    }

    private void doDeselect(T item, boolean userOriginated) {
        Set<T> oldSelection = new LinkedHashSet<>(selected);
        boolean removed = selected.remove(item);
        if (removed) {
            fireSelectionEvent(new MultiSelectionEvent<Grid<T>, T>(
                    grid, grid.asMultiSelect(), oldSelection,
                    userOriginated));
        }
    }
    
    private void doUpdateSelection(Set<T> addedItems,
            Set<T> removedItems, boolean userOriginated) {
        addedItems.removeIf(removedItems::remove);
        if (selected.containsAll(addedItems) && Collections
                .disjoint(selected, removedItems)) {
            return;
        }
        Set<T> oldSelection = new LinkedHashSet<>(selected);
        selected.removeAll(removedItems);
        selected.addAll(addedItems);
        grid.getDataCommunicator().reset();
        fireSelectionEvent(new MultiSelectionEvent<Grid<T>, T>(grid,
                grid.asMultiSelect(), oldSelection, userOriginated));
        if (removedItems.size() > 0) {
            selectionColumn.setSelectAllCheckboxState(false);
        }
    }
}
