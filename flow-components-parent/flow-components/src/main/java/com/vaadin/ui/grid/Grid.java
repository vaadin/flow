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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.data.AbstractListing;
import com.vaadin.data.Binder;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.provider.ArrayUpdater;
import com.vaadin.data.provider.ArrayUpdater.Update;
import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.selection.MultiSelect;
import com.vaadin.data.selection.MultiSelectionEvent;
import com.vaadin.data.selection.MultiSelectionListener;
import com.vaadin.data.selection.SelectionEvent;
import com.vaadin.data.selection.SelectionListener;
import com.vaadin.data.selection.SelectionModel;
import com.vaadin.data.selection.SelectionModel.Single;
import com.vaadin.data.selection.SingleSelect;
import com.vaadin.data.selection.SingleSelectionEvent;
import com.vaadin.data.selection.SingleSelectionListener;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.util.HtmlUtils;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.function.SerializableConsumer;
import com.vaadin.function.ValueProvider;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.AttachEvent;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.renderers.TemplateRenderer;
import com.vaadin.util.JsonSerializer;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side component for the {@code <vaadin-grid>} element.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the grid bean type
 *
 */
@Tag("vaadin-grid")
@HtmlImport("frontend://bower_components/vaadin-grid/vaadin-grid.html")
@JavaScript("context://gridConnector.js")
public class Grid<T> extends AbstractListing<T> implements HasDataProvider<T> {

    private final class UpdateQueue implements Update {
        private List<Runnable> queue = new ArrayList<>();

        private UpdateQueue(int size) {
            enqueue("connectorUpdateSize", size);
        }

        @Override
        public void set(int start, List<JsonValue> items) {
            enqueue("connectorSet", start,
                    items.stream().collect(JsonUtils.asArray()));
        }

        @Override
        public void clear(int start, int length) {
            enqueue("connectorClear", start, length);
        }

        @Override
        public void commit(int updateId) {
            enqueue("connectorConfirm", updateId);
            queue.forEach(Runnable::run);
            queue.clear();
        }

        private void enqueue(String name, Serializable... arguments) {
            queue.add(() -> getElement().callFunction(name, arguments));
        }
    }

    /**
     * Selection mode representing the built-in selection models in grid.
     * <p>
     * These enums can be used in {@link Grid#setSelectionMode(SelectionMode)}
     * to easily switch between the built-in selection models.
     *
     * @see Grid#setSelectionMode(SelectionMode)
     * @see Grid#setSelectionModel(GridSelectionModel, SelectionMode)
     */
    public enum SelectionMode {

        /**
         * Single selection mode that maps to built-in {@link Single}.
         *
         * @see GridSingleSelectionModel
         */
        SINGLE {
            @Override
            protected <T> GridSelectionModel<T> createModel(Grid<T> grid) {
                return new GridSingleSelectionModel<T>() {

                    private T selectedItem = null;
                    private boolean deselectAllowed = true;

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
                        grid.getDataCommunicator().reset();
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
                    public void remove() {
                        deselectAll();
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
                                return getSelectedItem()
                                        .orElse(getEmptyValue());
                            }

                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            @Override
                            public Registration addValueChangeListener(
                                    ValueChangeListener<Grid<T>, T> listener) {
                                Objects.requireNonNull(listener,
                                        "listener cannot be null");
                                return grid.addListener(
                                        SingleSelectionEvent.class,
                                        (ComponentEventListener) listener);
                            }

                            @Override
                            public Grid<T> get() {
                                return grid;
                            }
                        };
                    }

                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    @Override
                    public Registration addSelectionListener(
                            SelectionListener<T> listener) {
                        Objects.requireNonNull(listener,
                                "listener cannot be null");
                        return grid.addListener(SingleSelectionEvent.class,
                                (ComponentEventListener) (event -> listener
                                        .selectionChange(
                                                (SelectionEvent) event)));
                    }

                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    @Override
                    public Registration addSingleSelectionListener(
                            SingleSelectionListener<Grid<T>, T> listener) {
                        Objects.requireNonNull(listener,
                                "listener cannot be null");
                        return grid.addListener(SingleSelectionEvent.class,
                                (ComponentEventListener) (event -> listener
                                        .selectionChange(
                                                (SingleSelectionEvent) event)));
                    }

                    private void doSelect(T item, boolean userOriginated) {
                        T oldValue = selectedItem;
                        selectedItem = item;
                        grid.fireEvent(new SingleSelectionEvent<Grid<T>, T>(
                                grid, grid.asSingleSelect(), oldValue,
                                userOriginated));
                    }
                };
            }
        },

        /**
         * Multiselection mode that maps to built-in
         * {@link SelectionModel.Multi}.
         *
         * @see GridMultiSelectionModel
         */
        MULTI {
            @Override
            protected <T> GridSelectionModel<T> createModel(Grid<T> grid) {
                return new GridMultiSelectionModel<T>() {

                    Set<T> selected = new LinkedHashSet<>();

                    @Override
                    public void remove() {
                        deselectAll();
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
                    }

                    @Override
                    public void deselectAll() {
                        selected.clear();
                    }

                    @Override
                    public void updateSelection(Set<T> addedItems,
                            Set<T> removedItems) {
                        Objects.requireNonNull(addedItems,
                                "added items cannot be null");
                        Objects.requireNonNull(removedItems,
                                "removed items cannot be null");
                        addedItems.removeIf(removedItems::remove);
                        if (selected.containsAll(addedItems) && Collections
                                .disjoint(selected, removedItems)) {
                            return;
                        }
                        Set<T> oldSelection = new LinkedHashSet<>(selected);
                        selected.removeAll(removedItems);
                        selected.addAll(addedItems);
                        grid.getDataCommunicator().reset();
                        grid.fireEvent(new MultiSelectionEvent<Grid<T>, T>(grid,
                                grid.asMultiSelect(), oldSelection, false));
                    }

                    @Override
                    public void selectAll() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
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

                    private void doSelect(T item, boolean userOriginated) {
                        Set<T> oldSelection = new LinkedHashSet<>(selected);
                        boolean added = selected.add(item);
                        if (added) {
                            grid.fireEvent(new MultiSelectionEvent<Grid<T>, T>(
                                    grid, grid.asMultiSelect(), oldSelection,
                                    userOriginated));
                        }
                    }

                    private void doDeselect(T item, boolean userOriginated) {
                        Set<T> oldSelection = new LinkedHashSet<>(selected);
                        boolean removed = selected.remove(item);
                        if (removed) {
                            grid.fireEvent(new MultiSelectionEvent<Grid<T>, T>(
                                    grid, grid.asMultiSelect(), oldSelection,
                                    userOriginated));
                        }
                    }
                };
            }
        },

        /**
         * Selection model that doesn't allow selection.
         *
         * @see GridNoneSelectionModel
         */
        NONE {
            @Override
            protected <T> GridSelectionModel<T> createModel(Grid<T> grid) {
                return new GridNoneSelectionModel<>();
            }
        };

        /**
         * Creates the selection model to use with this enum.
         *
         * @param <T>
         *            the type of items in the grid
         * @param grid
         *            the grid to create the selection model for
         * @return the selection model
         */
        protected abstract <T> GridSelectionModel<T> createModel(Grid<T> grid);
    }

    private static final int PAGE_SIZE = 100;

    private final ArrayUpdater arrayUpdater = UpdateQueue::new;

    private final Map<String, Function<T, JsonValue>> columnGenerators = new HashMap<>();
    private final DataCommunicator<T> dataCommunicator = new DataCommunicator<>(
            this::generateItemJson, arrayUpdater, getElement().getNode());

    private int nextColumnId = 0;

    private GridSelectionModel<T> selectionModel = SelectionMode.SINGLE
            .createModel(this);

    /**
     * Creates a new instance.
     */
    public Grid() {
        getDataCommunicator().setRequestedRange(0, PAGE_SIZE);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().executeJavaScript(
                "window.gridConnector.initLazy($0, $1)", getElement(),
                PAGE_SIZE);
    }

    /**
     * Adds a new text column to this {@link Grid} with a value provider. The
     * value is converted to a JSON value by using
     * {@link JsonSerializer#toJson(Object)}.
     *
     * @param header
     *            the column header name
     * @param valueProvider
     *            the value provider
     */
    public void addColumn(String header, ValueProvider<T, ?> valueProvider) {
        String columnKey = getColumnKey(false);
        addColumn(header, TemplateRenderer.<T> of("[[item." + columnKey + "]]")
                .withProperty(columnKey, valueProvider));
    }

    /**
     * Adds a new text column to this {@link Grid} with a template renderer. The
     * values inside the renderer are converted to JSON values by using
     * {@link JsonSerializer#toJson(Object)}.
     * 
     * @param header
     *            the column header name
     * @param renderer
     *            the renderer used to create the grid cell structure
     * 
     * @see TemplateRenderer#of(String)
     */
    public void addColumn(String header, TemplateRenderer<T> renderer) {
        String columnKey = getColumnKey(true);

        renderer.getValueProviders().forEach((key, provider) -> {
            columnGenerators.put(key, provider.andThen(JsonSerializer::toJson));
        });

        getDataCommunicator().reset();

        // Use innerHTML to set document fragment instead of DOM children
        Element headerTemplate = new Element("template")
                .setAttribute("class", "header")
                .setProperty("innerHTML", HtmlUtils.escape(header));
        Element contentTemplate = new Element("template")
                .setProperty("innerHTML", renderer.getTemplate());

        Element colElement = new Element("vaadin-grid-column")
                .setAttribute("id", columnKey)
                .appendChild(headerTemplate, contentTemplate);

        Map<String, SerializableConsumer<T>> eventConsumers = renderer
                .getEventConsumers();

        if (!eventConsumers.isEmpty()) {
            /*
             * This code allows the template to use Polymer specific syntax for
             * events, such as on-click (instead of the native onclick). The
             * principle is: we set a new function inside the column, and the
             * function is called by the rendered template. For that to work,
             * the template element must have the "__dataHost" property set with
             * the column element.
             */
            colElement.getNode().runWhenAttached(ui -> {
                eventConsumers.forEach((handlerName,
                        consumer) -> setupTemplateRendererEventHandler(ui,
                                colElement, handlerName, consumer));
            });
            contentTemplate.getNode()
                    .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                            "$0.__dataHost = $1;", contentTemplate,
                            colElement));
        }

        getElement().appendChild(colElement);
    }

    private void setupTemplateRendererEventHandler(UI ui,
            Element colElement, String handlerName, Consumer<T> consumer) {

        // vaadin.sendEventMessage is an exported function at the client side
        ui.getPage().executeJavaScript(String.format(
                "$0.%s = function(e) {vaadin.sendEventMessage(%d, '%s', {key: e.model.__data.item.key})}",
                handlerName, colElement.getNode().getId(), handlerName),
                colElement);

        colElement.addEventListener(handlerName, event -> {
            if (event.getEventData() != null) {
                String itemKey = event.getEventData().getString("key");
                T item = getDataCommunicator().getKeyMapper().get(itemKey);

                if (item != null) {
                    consumer.accept(item);
                } else {
                    Logger.getLogger(Grid.class.getName()).log(Level.INFO,
                            String.format(
                                    "Received an event for the handler '%s' with item key '%s', but the item is not present in the KeyMapper. Ignoring event.",
                                    handlerName, itemKey));
                }
            }
        });
    }

    private String getColumnKey(boolean increment) {
        int id = nextColumnId;
        if (increment) {
            nextColumnId++;
        }
        return "col" + id;
    }

    @Override
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        getDataCommunicator().setDataProvider(dataProvider, null);
    }

    @Override
    public DataCommunicator<T> getDataCommunicator() {
        return dataCommunicator;
    }

    /**
     * Gets the current page size.
     *
     * @return the current page size
     */
    public int getPageSize() {
        return PAGE_SIZE;
    }

    /**
     * Returns the selection model for this grid.
     *
     * @return the selection model, not null
     */
    public GridSelectionModel<T> getSelectionModel() {
        assert selectionModel != null : "No selection model set by "
                + getClass().getName() + " constructor";
        return selectionModel;
    }

    /**
     * Sets the selection model for the grid.
     * <p>
     * This method is for setting a custom selection model, and is
     * {@code protected} because {@link #setSelectionMode(SelectionMode)} should
     * be used for easy switching between built-in selection models.
     * <p>
     * The default selection model is {@link GridSingleSelectionModel}.
     * <p>
     * To use a custom selection model, you can e.g. extend the grid call this
     * method with your custom selection model.
     *
     * @param model
     *            the selection model to use, not {@code null}
     * @param selectionMode
     *            the selection mode this selection model corresponds to, not
     *            {@code null}
     *
     * @see #setSelectionMode(SelectionMode)
     */
    protected void setSelectionModel(GridSelectionModel<T> model,
            SelectionMode selectionMode) {
        Objects.requireNonNull(model, "selection model cannot be null");
        Objects.requireNonNull(selectionMode, "selection mode cannot be null");
        selectionModel.remove();
        selectionModel = model;
        getElement().callFunction("setSelectionMode", selectionMode.name());
    }

    /**
     * Sets the grid's selection mode.
     * <p>
     * To use your custom selection model, you can use
     * {@link #setSelectionModel(GridSelectionModel, SelectionMode)}, see
     * existing selection model implementations for example.
     *
     * @param selectionMode
     *            the selection mode to switch to, not {@code null}
     * @return the used selection model
     *
     * @see SelectionMode
     * @see GridSelectionModel
     * @see #setSelectionModel(GridSelectionModel, SelectionMode)
     */
    public GridSelectionModel<T> setSelectionMode(SelectionMode selectionMode) {
        Objects.requireNonNull(selectionMode, "Selection mode cannot be null.");
        GridSelectionModel<T> model = selectionMode.createModel(this);
        setSelectionModel(model, selectionMode);
        return model;
    }

    /**
     * Use this grid as a single select in {@link Binder}.
     * <p>
     * Throws {@link IllegalStateException} if the grid is not using a
     * {@link GridSingleSelectionModel}.
     *
     * @return the single select wrapper that can be used in binder
     * @throws IllegalStateException
     *             if not using a single selection model
     */
    public SingleSelect<Grid<T>, T> asSingleSelect() {
        GridSelectionModel<T> model = getSelectionModel();
        if (!(model instanceof GridSingleSelectionModel)) {
            throw new IllegalStateException(
                    "Grid is not in single select mode, "
                            + "it needs to be explicitly set to such with "
                            + "setSelectionMode(SelectionMode.SINGLE) before "
                            + "being able to use single selection features.");
        }
        return ((GridSingleSelectionModel<T>) model).asSingleSelect();
    }

    /**
     * Use this grid as a multiselect in {@link Binder}.
     * <p>
     * Throws {@link IllegalStateException} if the grid is not using a
     * {@link GridMultiSelectionModel}.
     *
     * @return the multiselect wrapper that can be used in binder
     * @throws IllegalStateException
     *             if not using a multiselection model
     */
    public MultiSelect<Grid<T>, T> asMultiSelect() {
        GridSelectionModel<T> model = getSelectionModel();
        if (!(model instanceof GridMultiSelectionModel)) {
            throw new IllegalStateException("Grid is not in multi select mode, "
                    + "it needs to be explicitly set to such with "
                    + "setSelectionMode(SelectionMode.MULTI) before "
                    + "being able to use multi selection features.");
        }
        return ((GridMultiSelectionModel<T>) model).asMultiSelect();
    }

    /**
     * This method is a shorthand that delegates to the currently set selection
     * model.
     *
     * @see #getSelectionModel()
     * @see GridSelectionModel
     * 
     * @return a set with the selected items, never <code>null</code>
     */
    public Set<T> getSelectedItems() {
        return getSelectionModel().getSelectedItems();
    }

    /**
     * This method is a shorthand that delegates to the currently set selection
     * model.
     *
     * @param item
     *            the item to select
     *
     * @see #getSelectionModel()
     * @see GridSelectionModel
     */
    public void select(T item) {
        getSelectionModel().select(item);
    }

    /**
     * This method is a shorthand that delegates to the currently set selection
     * model.
     * 
     * @param item
     *            the item to deselect
     *
     * @see #getSelectionModel()
     * @see GridSelectionModel
     */
    public void deselect(T item) {
        getSelectionModel().deselect(item);
    }

    /**
     * This method is a shorthand that delegates to the currently set selection
     * model.
     *
     * @see #getSelectionModel()
     * @see GridSelectionModel
     */
    public void deselectAll() {
        getSelectionModel().deselectAll();
    }

    /**
     * Adds a selection listener to the current selection model.
     * <p>
     * This is a shorthand for
     * {@code grid.getSelectionModel().addSelectionListener()}. To get more
     * detailed selection events, use {@link #getSelectionModel()} and either
     * {@link GridSingleSelectionModel#addSingleSelectionListener(SingleSelectionListener)}
     * or
     * {@link GridMultiSelectionModel#addMultiSelectionListener(MultiSelectionListener)}
     * depending on the used selection mode.
     *
     * @param listener
     *            the listener to add
     * @return a registration handle to remove the listener
     * @throws UnsupportedOperationException
     *             if selection has been disabled with
     *             {@link SelectionMode#NONE}
     */
    public Registration addSelectionListener(SelectionListener<T> listener) {
        return getSelectionModel().addSelectionListener(listener);
    }

    @ClientDelegate
    private void select(int key) {
        getSelectionModel().selectFromClient(findByKey(key));
    }

    @ClientDelegate
    private void deselect(int key) {
        getSelectionModel().deselectFromClient(findByKey(key));
    }

    private T findByKey(int key) {
        T item = getDataCommunicator().getKeyMapper().get(String.valueOf(key));
        if (item == null) {
            throw new IllegalStateException("Unkonwn key: " + key);
        }
        return item;
    }

    private JsonValue generateItemJson(String key, T item) {
        JsonObject json = Json.createObject();
        json.put("key", key);
        columnGenerators.forEach((columnKey, generator) -> json.put(columnKey,
                generator.apply(item)));
        if (getSelectionModel().isSelected(item)) {
            json.put("selected", true);
        }
        return json;
    }

    @ClientDelegate
    private void confirmUpdate(int id) {
        getDataCommunicator().confirmUpdate(id);
    }

    @ClientDelegate
    private void setRequestedRange(int start, int length) {
        getDataCommunicator().setRequestedRange(start, length);
    }
}
