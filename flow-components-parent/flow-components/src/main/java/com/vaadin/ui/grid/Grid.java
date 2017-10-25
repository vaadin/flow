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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.data.AbstractListing;
import com.vaadin.data.Binder;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.provider.ArrayUpdater;
import com.vaadin.data.provider.ArrayUpdater.Update;
import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.data.selection.MultiSelect;
import com.vaadin.data.selection.MultiSelectionListener;
import com.vaadin.data.selection.SelectionEvent;
import com.vaadin.data.selection.SelectionListener;
import com.vaadin.data.selection.SelectionModel;
import com.vaadin.data.selection.SelectionModel.Single;
import com.vaadin.data.selection.SingleSelect;
import com.vaadin.data.selection.SingleSelectionEvent;
import com.vaadin.data.selection.SingleSelectionListener;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.util.HtmlUtils;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.function.SerializableConsumer;
import com.vaadin.function.ValueProvider;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.Focusable;
import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.event.Synchronize;
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
@HtmlImport("frontend://bower_components/vaadin-grid/vaadin-grid-column.html")
@HtmlImport("frontend://bower_components/vaadin-checkbox/vaadin-checkbox.html")
@JavaScript("context://gridConnector.js")
public class Grid<T> extends AbstractListing<T>
        implements HasDataProvider<T>, HasStyle, HasSize, Focusable<Grid<T>> {

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
                return new AbstractGridMultiSelectionModel<T>(grid) {

                    @Override
                    protected void fireSelectionEvent(SelectionEvent<T> event) {
                        grid.fireEvent((ComponentEvent<Grid>) event);
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

    /**
     * Server-side component for the {@code <vaadin-grid-column>} element.
     * 
     * @param <T>
     *            type of the underlying grid this column is compatible with
     */
    @Tag("vaadin-grid-column")
    public static class Column<T> extends Component {

        private final Grid<T> grid;

        /**
         * Constructs a new Column for use inside a Grid.
         *
         * @param grid
         *            the grid this column is attached to
         * @param columnId
         *            unique identifier of this column
         * @param header
         *            the header text of this column
         * @param renderer
         *            the renderer to use in this column
         */
        public Column(Grid<T> grid, String columnId, String header,
                TemplateRenderer<T> renderer) {
            this.grid = grid;

            Element headerTemplate = new Element("template")
                    .setAttribute("class", "header")
                    .setProperty("innerHTML", HtmlUtils.escape(header));

            Element contentTemplate = new Element("template")
                    .setProperty("innerHTML", renderer.getTemplate());

            getElement().setAttribute("id", columnId)
                    .appendChild(headerTemplate, contentTemplate);

            Map<String, SerializableConsumer<T>> eventConsumers = renderer
                    .getEventHandlers();

            if (!eventConsumers.isEmpty()) {
                /*
                 * This code allows the template to use Polymer specific syntax
                 * for events, such as on-click (instead of the native onclick).
                 * The principle is: we set a new function inside the column,
                 * and the function is called by the rendered template. For that
                 * to work, the template element must have the "__dataHost"
                 * property set with the column element.
                 */
                getElement().getNode().runWhenAttached(
                        ui -> processTemplateRendererEventConsumers(ui,
                                getElement(), eventConsumers));

                contentTemplate.getNode()
                        .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                                "$0.__dataHost = $1;", contentTemplate,
                                getElement()));
            }
        }

        /**
         * Sets the width of this column as a CSS-string.
         *
         * @see #setFlexGrow(int)
         *
         * @param width
         *            the width to set this column to, as a CSS-string, not
         *            {@code null}
         * @return this column, for method chaining
         */
        public Column<T> setWidth(String width) {
            getElement().setProperty("width", width);
            return this;
        }

        /**
         * Gets the width of this column as a CSS-string.
         * 
         * @return the width of this column as a CSS-string
         */
        @Synchronize("width-changed")
        public String getWidth() {
            return getElement().getProperty("width");
        }

        /**
         * Sets the flex grow ratio for this column. When set to 0, column width
         * is fixed.
         *
         * @see #setWidth(String)
         *
         * @param flexGrow
         * @return this column, for method chaining
         */
        public Column<T> setFlexGrow(int flexGrow) {
            getElement().setProperty("flexGrow", flexGrow);
            return this;
        }

        /**
         * Gets the currently set flex grow value, by default 1.
         *
         * @return the currently set flex grow value, by default 1
         */
        @Synchronize("flex-grow-changed")
        public int getFlexGrow() {
            return getElement().getProperty("flexGrow", 1);
        }


        /**
         * When set to {@code true}, the column is user-resizable. By default
         * this is set to {@code false}.
         *
         * @param resizable
         *            whether to allow user resizing of this column
         * @return this column, for method chaining
         */
        public Column<T> setResizable(boolean resizable) {
            getElement().setProperty("resizable", resizable);
            return this;
        }

        /**
         * Gets whether this column is user-resizable.
         *
         * @return whether this column is user-resizable
         */
        @Synchronize("resizable-changed")
        public boolean isResizable() {
            return getElement().getProperty("resizable", false);
        }

        /**
         * Hides or shows the column. By default columns are visible before
         * explicitly hiding them.
         *
         * @param hidden
         *            {@code true} to hide the column, {@code false} to show
         * @return this column, for method chaining
         */
        public Column<T> setHidden(boolean hidden) {
            getElement().setProperty("hidden", hidden);
            return this;
        }

        /**
         * Returns whether this column is hidden. Default is {@code false}.
         *
         * @return {@code true} if the column is currently hidden, {@code false}
         *         otherwise
         */
        @Synchronize("hidden-changed")
        public boolean isHidden() {
            return getElement().getProperty("hidden", false);
        }

        /**
         * Gets the underlying {@code <vaadin-grid-column>} element.
         * <p>
         * <strong>It is highly discouraged to directly use the API exposed by
         * the returned element.</strong>
         *
         * @return the root element of this component
         */
        @Override
        public Element getElement() {
            return super.getElement();
        }

        private void processTemplateRendererEventConsumers(UI ui,
                Element colElement,
                Map<String, SerializableConsumer<T>> eventConsumers) {
            eventConsumers.forEach((handlerName,
                    consumer) -> setupTemplateRendererEventHandler(ui,
                            colElement, handlerName, consumer));
        }

        private void setupTemplateRendererEventHandler(UI ui,
                Element colElement, String handlerName, Consumer<T> consumer) {

            // vaadin.sendEventMessage is an exported function at the client
            // side
            ui.getPage()
                    .executeJavaScript(String.format(
                            "$0.%s = function(e) {vaadin.sendEventMessage(%d, '%s', {key: e.model.__data.item.key})}",
                            handlerName, colElement.getNode().getId(),
                            handlerName), colElement);

            colElement.addEventListener(handlerName,
                    event -> processEventFromTemplateRenderer(event,
                            handlerName, consumer));
        }

        private void processEventFromTemplateRenderer(DomEvent event,
                String handlerName, Consumer<T> consumer) {
            if (event.getEventData() != null) {
                String itemKey = event.getEventData().getString("key");
                T item = getGrid().getDataCommunicator().getKeyMapper()
                        .get(itemKey);

                if (item != null) {
                    consumer.accept(item);
                } else {
                    Logger.getLogger(getClass().getName()).log(Level.INFO,
                            () -> String.format(
                                    "Received an event for the handler '%s' with item key '%s', but the item is not present in the KeyMapper. Ignoring event.",
                                    handlerName, itemKey));
                }
            } else {
                Logger.getLogger(getClass().getName()).log(Level.INFO,
                        () -> String.format(
                                "Received an event for the handler '%s' without any data. Ignoring event.",
                                handlerName));
            }
        }

        /**
         * Gets the grid that this column belongs to.
         *
         * @return the grid that this column belongs to
         */
        private Grid<T> getGrid() {
            return grid;
        }
    }

    private final ArrayUpdater arrayUpdater = UpdateQueue::new;

    private final Map<String, Function<T, JsonValue>> columnGenerators = new HashMap<>();
    private final DataCommunicator<T> dataCommunicator = new DataCommunicator<>(
            this::generateItemJson, arrayUpdater,
            data -> getElement().callFunction("updateData", data),
            getElement().getNode());

    private int nextColumnId = 0;

    private GridSelectionModel<T> selectionModel = SelectionMode.SINGLE
            .createModel(this);

    /**
     * Creates a new instance, with page size of 50.
     */
    public Grid() {
        this(50);
    }

    /**
     * Creates a new instance, with the specified page size.
     * <p>
     * The page size influences the {@link Query#getLimit()} sent by the client,
     * but it's up to the webcomponent to determine the actual query limit,
     * based on the height of the component and scroll position. Usually the
     * limit is 3 times the page size (e.g. 150 items with a page size of 50).
     * 
     * @param pageSize
     *            the page size. Must be greater than zero.
     */
    public Grid(int pageSize) {
        setPageSize(pageSize);

        getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                        "window.gridConnector.initLazy($0)", getElement()));
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
     * @return the created column
     */
    public Column<T> addColumn(String header,
            ValueProvider<T, ?> valueProvider) {
        String columnKey = getColumnKey(false);
        return addColumn(header,
                TemplateRenderer.<T> of("[[item." + columnKey + "]]")
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
     * @return the created column
     *
     * @see TemplateRenderer#of(String)
     */
    public Column<T> addColumn(String header,
            TemplateRenderer<T> renderer) {
        String columnKey = getColumnKey(true);

        renderer.getValueProviders().forEach((key, provider) -> {
            columnGenerators.put(key, provider.andThen(JsonSerializer::toJson));
        });

        getDataCommunicator().reset();
        
        Column<T> column = new Column<>(this, columnKey, header, renderer);

        getElement().getNode().runWhenAttached(
                ui -> getElement().appendChild(column.getElement()));

        return column;
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
        Objects.requireNonNull(dataProvider, "data provider cannot be null");
        getDataCommunicator().setDataProvider(dataProvider, null);
    }

    /**
     * Returns the data provider of this grid.
     *
     * @return the data provider of this grid, not {@code null}
     */
    public DataProvider<T, ?> getDataProvider() {
        return getDataCommunicator().getDataProvider();
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
        return getElement().getProperty("pageSize", 50);
    }

    /**
     * Sets the page size.
     * <p>
     * This method is private at the moment because the Grid doesn't support
     * changing the the pageSize after the initial load.
     * 
     * @param pageSize
     *            the maximum number of items sent per request. Should be
     *            greater than zero
     */
    private void setPageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException(
                    "The pageSize should be greater than zero. Was "
                            + pageSize);
        }
        getElement().setProperty("pageSize", pageSize);
        getDataCommunicator().setRequestedRange(0, pageSize);
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
