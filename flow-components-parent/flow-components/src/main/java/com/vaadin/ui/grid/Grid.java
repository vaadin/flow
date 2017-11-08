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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.data.AbstractListing;
import com.vaadin.data.Binder;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.provider.ArrayUpdater;
import com.vaadin.data.provider.ArrayUpdater.Update;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataChangeEvent.DataRefreshEvent;
import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.data.provider.DataGenerator;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.DataProviderListener;
import com.vaadin.data.provider.Query;
import com.vaadin.data.selection.MultiSelect;
import com.vaadin.data.selection.MultiSelectionListener;
import com.vaadin.data.selection.SelectionEvent;
import com.vaadin.data.selection.SelectionListener;
import com.vaadin.data.selection.SelectionModel;
import com.vaadin.data.selection.SelectionModel.Single;
import com.vaadin.data.selection.SingleSelect;
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
import com.vaadin.ui.event.ComponentEventBus;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.renderers.ComponentRendererUtil;
import com.vaadin.ui.renderers.TemplateRenderer;
import com.vaadin.util.JsonSerializer;

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
@HtmlImport("context://flow-component-renderer.html")
@JavaScript("context://gridConnector.js")
public class Grid<T> extends AbstractListing<T>
        implements HasDataProvider<T>, HasStyle, HasSize, Focusable<Grid<T>> {

    private final class UpdateQueue implements Update {
        private List<Runnable> queue = new ArrayList<>();

        private UpdateQueue(int size) {
            enqueue("$connector.updateSize", size);
        }

        @Override
        public void set(int start, List<JsonValue> items) {
            enqueue("$connector.set", start,
                    items.stream().collect(JsonUtils.asArray()));
            itemEventBus.fireEvent(new ItemsSentEvent(Grid.this, items));
        }

        @Override
        public void clear(int start, int length) {
            enqueue("$connector.clear", start, length);
        }

        @Override
        public void commit(int updateId) {
            enqueue("$connector.confirm", updateId);
            queue.forEach(Runnable::run);
            queue.clear();
        }

        private void enqueue(String name, Serializable... arguments) {
            queue.add(() -> getElement().callFunction(name, arguments));
        }
    }

    private static final class ItemsSentEvent extends ComponentEvent<Grid<?>> {

        private final List<JsonValue> items;

        public ItemsSentEvent(Grid<?> source, List<JsonValue> items) {
            super(source, false);
            this.items = items;
        }

        public List<JsonValue> getItems() {
            return items;
        }
    }

    /**
     * Internal event fired when DataProviders are changed in the Grid.
     */
    private static final class DataProviderChangedEvent
            extends ComponentEvent<Grid<?>> {

        /**
         * Default event constructor.
         */
        public DataProviderChangedEvent(Grid<?> source) {
            super(source, false);
        }
    }

    /**
     * Internal object to hold {@link ComponentRenderer}s and their generated
     * {@link Component}s together.
     * 
     * @param <T>
     *            the model item attached to the component
     */
    private static final class RendereredComponent<T> implements Serializable {
        private Component component;
        private ComponentRenderer<? extends Component, T> componentRenderer;

        /**
         * Default constructor.
         * 
         * @param component
         *            the generated component
         * @param componentRenderer
         *            the renderer that generated the component
         */
        public RendereredComponent(Component component,
                ComponentRenderer<? extends Component, T> componentRenderer) {
            this.component = component;
            this.componentRenderer = componentRenderer;
        }

        /**
         * Gets the current generated component.
         * 
         * @return the generated component by the renderer
         */
        public Component getComponent() {
            return component;
        }

        /**
         * Recreates the component by calling
         * {@link ComponentRenderer#createComponent(Object)}, and sets the
         * internal component returned by {@link #getComponent()}.
         * 
         * @param item
         *            the model item to be attached to the component instance
         * @return the new generated component returned by the renderer
         */
        public Component recreateComponent(T item) {
            component = componentRenderer.createComponent(item);
            return component;
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
                return new AbstractGridSingleSelectionModel<T>(grid) {

                    @Override
                    protected void fireSelectionEvent(SelectionEvent<T> event) {
                        grid.fireEvent((ComponentEvent<Grid>) event);
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
    public static class Column<T> extends Component
            implements ColumnBase<Column<T>> {

        private final Grid<T> grid;
        private Map<String, RendereredComponent<T>> renderedComponents;

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

            if (renderer instanceof ComponentRenderer) {
                renderedComponents = new HashMap<>();
                ComponentRenderer<? extends Component, T> componentRenderer = (ComponentRenderer<? extends Component, T>) renderer;
                grid.setupComponentRenderer(this, componentRenderer,
                        renderedComponents);
            }

            Element headerTemplate = new Element("template")
                    .setAttribute("class", "header")
                    .setProperty("innerHTML", HtmlUtils.escape(header));

            Element contentTemplate = new Element("template")
                    .setProperty("innerHTML", renderer.getTemplate());

            getElement().setAttribute("id", columnId)
                    .appendChild(headerTemplate, contentTemplate);

            getGrid().setupTemplateRenderer(renderer, contentTemplate,
                    getElement());
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

        /**
         * Gets the grid that this column belongs to.
         *
         * @return the grid that this column belongs to
         */
        private Grid<T> getGrid() {
            return grid;
        }
    }

    /**
     * A helper base class for creating extensions for the Grid component.
     *
     * @param <T>
     *            the grid bean type
     */
    public abstract static class AbstractGridExtension<T>
            implements DataGenerator<T> {

        private Grid<T> grid;

        /**
         * Constructs a new grid extension, extending the given grid.
         *
         * @param grid
         *            the grid to extend
         */
        public AbstractGridExtension(Grid<T> grid) {
            extend(grid);
        }

        /**
         * A helper method for refreshing the client-side representation of a
         * single data item.
         *
         * @param item
         *            the item to refresh
         */
        protected void refresh(T item) {
            getGrid().getDataCommunicator().refresh(item);
        }

        /**
         * Adds this extension to the given grid.
         * 
         * @param grid
         *            the grid to extend
         */
        protected void extend(Grid<T> grid) {
            this.grid = grid;
            getGrid().getDataGenerator().addDataGenerator(this);
        }

        /**
         * Remove this extension from its target.
         */
        protected void remove() {
            getGrid().getDataGenerator().removeDataGenerator(this);
        }

        /**
         * Gets the Grid this extension extends.
         * 
         * @return the grid this extension extends
         */
        protected Grid<T> getGrid() {
            return grid;
        }
    }

    /**
     * Data generator implementation for the Grid.
     * 
     * @param <T>
     *            the grid bean type
     */
    private class GridDataGenerator<T> implements DataGenerator<T> {

        private final Set<DataGenerator<T>> dataGenerators = new HashSet<>();

        @Override
        public void generateData(T item, JsonObject jsonObject) {
            dataGenerators.forEach(
                    generator -> generator.generateData(item, jsonObject));
        }

        @Override
        public void destroyData(T item) {
            dataGenerators.forEach(generator -> generator.destroyData(item));
        }

        @Override
        public void destroyAllData() {
            dataGenerators.forEach(DataGenerator::destroyAllData);
        }

        @Override
        public void refreshData(T item) {
            dataGenerators.forEach(generator -> generator.refreshData(item));
        }

        /**
         * Adds the given data generator. If the generator was already added,
         * does nothing.
         *
         * @param generator
         *            the data generator to add
         */
        public void addDataGenerator(DataGenerator<T> generator) {
            assert generator != null : "generator should not be null";
            dataGenerators.add(generator);
        }

        /**
         * Removes the given data generator.
         *
         * @param generator
         *            the data generator to remove
         */
        public void removeDataGenerator(DataGenerator<T> generator) {
            assert generator != null : "generator should not be null";
            dataGenerators.remove(generator);
        }
    }

    /**
     * Class for managing visible details rows.
     *
     * @param <T>
     *            the grid bean type
     */
    private class DetailsManager<T> extends AbstractGridExtension<T> {

        private final Set<T> detailsVisible = new HashSet<>();

        /**
         * Constructs a new details manager for the given grid.
         *
         * @param grid
         *            the grid whose details are to be managed
         */
        public DetailsManager(Grid<T> grid) {
            super(grid);
        }

        /**
         * Sets the visibility of details for given item.
         *
         * @param item
         *            the item to show details for
         * @param visible
         *            {@code true} if details component should be visible;
         *            {@code false} if it should be hidden
         */
        public void setDetailsVisible(T item, boolean visible) {
            boolean refresh = false;
            if (!visible) {
                refresh = detailsVisible.remove(item);
            } else {
                refresh = detailsVisible.add(item);
            }

            if (refresh) {
                refresh(item);
            }
        }

        /**
         * Returns the visibility of the details component for the given item.
         *
         * @param item
         *            the item to check
         *
         * @return {@code true} if details component should be visible;
         *         {@code false} if it should be hidden
         */
        public boolean isDetailsVisible(T item) {
            return detailsVisible.contains(item);
        }

        @Override
        public void generateData(T item, JsonObject jsonObject) {
            if (isDetailsVisible(item)) {
                jsonObject.put("detailsOpened", true);
            }
        }

        @Override
        public void destroyData(T item) {
            detailsVisible.remove(item);
        }

        @Override
        public void destroyAllData() {
            detailsVisible.clear();
        }

        private void setDetailsVisibleFromClient(Set<T> items) {
            detailsVisible.clear();
            detailsVisible.addAll(items);
        }
    }

    private final ArrayUpdater arrayUpdater = UpdateQueue::new;

    private final GridDataGenerator<T> gridDataGenerator = new GridDataGenerator<>();
    private final DataCommunicator<T> dataCommunicator = new DataCommunicator<>(
            gridDataGenerator, arrayUpdater,
            data -> getElement().callFunction("$connector.updateData", data),
            getElement().getNode());

    private int nextColumnId = 0;

    private ComponentEventBus itemEventBus = new ComponentEventBus(this);

    private GridSelectionModel<T> selectionModel;

    private final DetailsManager<T> detailsManager = new DetailsManager<>(this);
    private Element detailsTemplate;
    private Map<String, RendereredComponent<T>> renderedDetailComponents;

    private List<ColumnBase<?>> parentColumns = new ArrayList<>();

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
        setSelectionModel(SelectionMode.SINGLE.createModel(this),
                SelectionMode.SINGLE);

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
    public Column<T> addColumn(String header, TemplateRenderer<T> renderer) {
        String columnKey = getColumnKey(true);

        getDataCommunicator().reset();

        Column<T> column = new Column<>(this, columnKey, header, renderer);
        parentColumns.add(column);
        getElement().appendChild(column.getElement());

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
        itemEventBus.fireEvent(new DataProviderChangedEvent(this));
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
        if (selectionModel != null
                && selectionModel instanceof AbstractGridExtension) {
            ((AbstractGridExtension) selectionModel).remove();
        }
        selectionModel = model;
        getElement().callFunction("$connector.setSelectionMode",
                selectionMode.name());
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

    /**
     * Set the renderer to use for displaying the item details rows in this
     * grid.
     *
     * @param renderer
     *            the renderer to use for displaying item details rows,
     *            {@code null} to remove the current renderer
     */
    public void setItemDetailsRenderer(TemplateRenderer<T> renderer) {
        if (detailsTemplate != null) {
            getElement().removeChild(detailsTemplate);
        }
        if (renderer == null) {
            return;
        }
        if (renderer instanceof ComponentRenderer) {
            renderedDetailComponents = renderedDetailComponents == null
                    ? new HashMap<>()
                    : renderedDetailComponents;
            renderedDetailComponents
                    .forEach((key, rendereredComponent) -> rendereredComponent
                            .getComponent().getElement().removeFromParent());
            renderedDetailComponents.clear();

            ComponentRenderer<? extends Component, T> componentRenderer = (ComponentRenderer<? extends Component, T>) renderer;
            setupComponentRenderer(this, componentRenderer,
                    renderedDetailComponents);
        }

        Element newDetailsTemplate = new Element("template")
                .setAttribute("class", "row-details")
                .setProperty("innerHTML", renderer.getTemplate());

        detailsTemplate = newDetailsTemplate;
        getElement().appendChild(detailsTemplate);

        setupTemplateRenderer(renderer, detailsTemplate, getElement());
    }

    /**
     * Returns whether column reordering is allowed. Default value is
     * {@code false}.
     *
     * @return true if reordering is allowed
     */
    @Synchronize("column-reordering-allowed-changed")
    public boolean isColumnReorderingAllowed() {
        return getElement().getProperty("columnReorderingAllowed", false);
    }

    /**
     * Sets whether or not column reordering is allowed. Default value is
     * {@code false}.
     *
     * @param columnReorderingAllowed
     *            specifies whether column reordering is allowed
     */
    public void setColumnReorderingAllowed(boolean columnReorderingAllowed) {
        if (isColumnReorderingAllowed() != columnReorderingAllowed) {
            getElement().setProperty("columnReorderingAllowed",
                    columnReorderingAllowed);
        }
    }

    /**
     * Merges two or more columns into a {@link ColumnGroup}.
     *
     * @param header
     *            the header text of the resulting column group
     * @param firstColumn
     *            the first column to merge
     * @param secondColumn
     *            the second column to merge
     * @param additionalColumns
     *            optional additional columns to merge
     * @return the column group that contains the merged columns
     */
    public ColumnGroup mergeColumns(String header, ColumnBase<?> firstColumn,
            ColumnBase<?> secondColumn, ColumnBase<?>... additionalColumns) {
        List<ColumnBase<?>> toMerge = new ArrayList<>();
        toMerge.add(firstColumn);
        toMerge.add(secondColumn);
        toMerge.addAll(Arrays.asList(additionalColumns));
        return mergeColumns(header, toMerge);
    }

    /**
     * Merges two or more columns into a {@link ColumnGroup}.
     *
     * @param header
     *            the header text of the resulting column group
     * @param columnsToMerge
     *            the columns to merge, not {@code null} and size must be
     *            greater than 1
     * @return the column group that contains the merged columns
     */
    public ColumnGroup mergeColumns(String header,
            Collection<ColumnBase<?>> columnsToMerge) {
        Objects.requireNonNull(columnsToMerge,
                "Columns to merge cannot be null");
        if (columnsToMerge.size() < 2) {
            throw new IllegalArgumentException(
                    "Cannot merge less than two columns");
        }
        if (columnsToMerge.stream()
                .anyMatch(column -> !parentColumns.contains(column))) {
            throw new IllegalArgumentException(
                    "Cannot merge a column that is not a parent column of this grid");
        }

        int insertIndex = parentColumns
                .indexOf(columnsToMerge.iterator().next());

        columnsToMerge.forEach(column -> {
            getElement().removeChild(column.getElement());
            parentColumns.remove(column);
        });

        ColumnGroup columnGroup = new ColumnGroup(header, columnsToMerge);

        getElement().insertChild(insertIndex, columnGroup.getElement());
        parentColumns.add(insertIndex, columnGroup);

        return columnGroup;
    }

    /**
     * Gets an unmodifiable list of all parent columns currently in this
     * {@link Grid}. Parent columns are the top level columns of this Grid, i.e.
     * the topmost ColumnGroup
     *
     * @return unmodifiable list of parent columns
     */
    public List<ColumnBase<?>> getParentColumns() {
        return Collections.unmodifiableList(parentColumns);
    }

    /**
     * Gets an unmodifiable list of all {@link Column}s currently in this
     * {@link Grid}.
     *
     * @return unmodifiable list of columns
     */
    public List<Column<T>> getColumns() {
        List<Column<T>> ret = new ArrayList<>();
        getParentColumns().forEach(column -> appendChildColumns(ret, column));
        return Collections.unmodifiableList(ret);
    }

    /**
     * Sets the visibility of details component for given item.
     *
     * @param item
     *            the item to show details for
     * @param visible
     *            {@code true} if details component should be visible;
     *            {@code false} if it should be hidden
     */
    public void setDetailsVisible(T item, boolean visible) {
        detailsManager.setDetailsVisible(item, visible);
    }

    /**
     * Returns the visibility of details component for given item.
     *
     * @param item
     *            the item to show details for
     *
     * @return {@code true} if details component should be visible;
     *         {@code false} if it should be hidden
     */
    public boolean isDetailsVisible(T item) {
        return detailsManager.isDetailsVisible(item);
    }

    private List<Column<T>> fetchChildColumns(ColumnGroup columnGroup) {
        List<Column<T>> ret = new ArrayList<>();
        columnGroup.getChildColumns()
                .forEach(column -> appendChildColumns(ret, column));
        return ret;
    }

    private void appendChildColumns(List<Column<T>> list,
            ColumnBase<?> column) {
        if (column instanceof Column) {
            list.add((Column<T>) column);
        } else if (column instanceof ColumnGroup) {
            list.addAll(fetchChildColumns((ColumnGroup) column));
        }
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

    @ClientDelegate
    private void confirmUpdate(int id) {
        getDataCommunicator().confirmUpdate(id);
    }

    @ClientDelegate
    private void setRequestedRange(int start, int length) {
        getDataCommunicator().setRequestedRange(start, length);
    }

    @ClientDelegate
    private void setDetailsVisible(String key) {
        if (key == null) {
            detailsManager.setDetailsVisibleFromClient(Collections.emptySet());
        } else {
            detailsManager.setDetailsVisibleFromClient(Collections
                    .singleton(getDataCommunicator().getKeyMapper().get(key)));
        }
    }

    private void setupTemplateRenderer(TemplateRenderer<T> renderer,
            Element contentTemplate, Element templateDataHost) {

        renderer.getValueProviders()
                .forEach((key, provider) -> getDataGenerator().addDataGenerator(
                        (item, jsonObject) -> jsonObject.put(key,
                                JsonSerializer.toJson(provider.apply(item)))));

        Map<String, SerializableConsumer<T>> eventConsumers = renderer
                .getEventHandlers();

        if (!eventConsumers.isEmpty()) {
            /*
             * This code allows the template to use Polymer specific syntax for
             * events, such as on-click (instead of the native onclick). The
             * principle is: we set a new function inside the column, and the
             * function is called by the rendered template. For that to work,
             * the template element must have the "__dataHost" property set with
             * the column element.
             */
            templateDataHost.getNode().runWhenAttached(
                    ui -> processTemplateRendererEventConsumers(ui,
                            templateDataHost, eventConsumers));

            contentTemplate.getNode()
                    .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                            "$0.__dataHost = $1;", contentTemplate,
                            templateDataHost));
        }
    }

    private void processTemplateRendererEventConsumers(UI ui,
            Element colElement,
            Map<String, SerializableConsumer<T>> eventConsumers) {
        eventConsumers.forEach(
                (handlerName, consumer) -> setupTemplateRendererEventHandler(ui,
                        colElement, handlerName, consumer));
    }

    private void setupTemplateRendererEventHandler(UI ui, Element eventOrigin,
            String handlerName, Consumer<T> consumer) {

        // vaadin.sendEventMessage is an exported function at the client
        // side
        ui.getPage().executeJavaScript(String.format(
                "$0.%s = function(e) {vaadin.sendEventMessage(%d, '%s', {key: e.model.__data.item.key})}",
                handlerName, eventOrigin.getNode().getId(), handlerName),
                eventOrigin);

        eventOrigin.addEventListener(handlerName,
                event -> processEventFromTemplateRenderer(event, handlerName,
                        consumer));
    }

    private void processEventFromTemplateRenderer(DomEvent event,
            String handlerName, Consumer<T> consumer) {
        if (event.getEventData() != null) {
            String itemKey = event.getEventData().getString("key");
            T item = getDataCommunicator().getKeyMapper().get(itemKey);

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

    private void setupComponentRenderer(Component owner,
            ComponentRenderer<? extends Component, T> componentRenderer,
            Map<String, RendereredComponent<T>> renderedComponents) {

        Element container = ComponentRendererUtil
                .createContainerForRenderers(owner);

        componentRenderer.setTemplateAttribute("key", "[[item.key]]");
        componentRenderer.setTemplateAttribute("keyname",
                "data-flow-renderer-item-key");
        componentRenderer.setTemplateAttribute("containerid",
                container.getAttribute("id"));

        DataProviderListener<T> dataProviderListener = event -> onDataChangeEvent(
                event, componentRenderer, renderedComponents, container);

        getDataProvider().addDataProviderListener(dataProviderListener);

        itemEventBus.addListener(Grid.DataProviderChangedEvent.class,
                event -> getDataProvider()
                        .addDataProviderListener(dataProviderListener));

        itemEventBus.addListener(ItemsSentEvent.class,
                event -> onItemsSent(event.getItems(), container,
                        componentRenderer, renderedComponents));
    }

    private void onDataChangeEvent(DataChangeEvent<T> event,
            ComponentRenderer<? extends Component, T> componentRenderer,
            Map<String, RendereredComponent<T>> renderedComponents,
            Element container) {

        if (event instanceof DataRefreshEvent) {
            // this event is fired when a single item is refreshed on the
            // DataProvider
            onDataRefreshEvent((DataRefreshEvent<T>) event, componentRenderer,
                    renderedComponents, container);
        } else {
            // otherwise the DataProvider was entirely renewed, so we need to
            // clear everything
            container.removeAllChildren();
            renderedComponents.clear();
        }
    }

    private void onDataRefreshEvent(DataRefreshEvent<T> event,
            ComponentRenderer<? extends Component, T> componentRenderer,
            Map<String, RendereredComponent<T>> renderedComponents,
            Element container) {

        T item = event.getItem();
        String key = getDataCommunicator().getKeyMapper().key(item);
        RendereredComponent<T> rendereredComponent = renderedComponents
                .get(key);
        if (rendereredComponent != null) {
            Component old = rendereredComponent.component;
            Component recreatedComponent = rendereredComponent
                    .recreateComponent(item);

            if (old.getElement().getNode().getId() != recreatedComponent
                    .getElement().getNode().getId()) {

                ComponentRendererUtil.removeRendereredComponent(UI.getCurrent(),
                        container,
                        "[data-flow-renderer-item-key='" + key + "']");

                registerRenderedComponent(componentRenderer, renderedComponents,
                        container, key, recreatedComponent);
            }
        }
    }

    private void onItemsSent(List<JsonValue> items, Element container,
            ComponentRenderer<? extends Component, T> componentRenderer,
            Map<String, RendereredComponent<T>> renderedComponents) {
        items.stream().map(value -> ((JsonObject) value).getString("key"))
                .filter(key -> !renderedComponents.containsKey(key))
                .forEach(key -> {
                    Component renderedComponent = componentRenderer
                            .createComponent(getDataCommunicator()
                                    .getKeyMapper().get(key));
                    registerRenderedComponent(componentRenderer,
                            renderedComponents, container, key,
                            renderedComponent);
                });
    }

    private void registerRenderedComponent(
            ComponentRenderer<? extends Component, T> componentRenderer,
            Map<String, RendereredComponent<T>> renderedComponents,
            Element container, String key, Component component) {
        component.getElement().setAttribute("data-flow-renderer-item-key", key);
        container.appendChild(component.getElement());

        renderedComponents.put(key,
                new RendereredComponent<>(component, componentRenderer));
    }

    private GridDataGenerator<T> getDataGenerator() {
        return gridDataGenerator;
    }
}
