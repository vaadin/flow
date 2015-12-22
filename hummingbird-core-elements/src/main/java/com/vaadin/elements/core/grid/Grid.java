/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.elements.core.grid;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.annotations.Bower;
import com.vaadin.annotations.Implemented;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.Tag;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Container.ItemSetChangeNotifier;
import com.vaadin.data.Container.PropertySetChangeEvent;
import com.vaadin.data.Container.PropertySetChangeListener;
import com.vaadin.data.Container.PropertySetChangeNotifier;
import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.fieldgroup.FieldGroupFieldFactory;
import com.vaadin.data.sort.Sort;
import com.vaadin.data.sort.SortOrder;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.elements.core.grid.event.ColumnReorderEvent;
import com.vaadin.elements.core.grid.event.ColumnVisibilityChangeEvent;
import com.vaadin.elements.core.grid.event.ColumnVisibilityChangeListener;
import com.vaadin.elements.core.grid.event.CommitErrorEvent;
import com.vaadin.elements.core.grid.event.EditorErrorHandler;
import com.vaadin.elements.core.grid.headerfooter.Footer;
import com.vaadin.elements.core.grid.headerfooter.FooterCell;
import com.vaadin.elements.core.grid.headerfooter.FooterRow;
import com.vaadin.elements.core.grid.headerfooter.Header;
import com.vaadin.elements.core.grid.headerfooter.HeaderCell;
import com.vaadin.elements.core.grid.headerfooter.HeaderRow;
import com.vaadin.elements.core.grid.selection.SelectionMode;
import com.vaadin.elements.core.grid.selection.SelectionModel;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ItemClickEvent.ItemClickNotifier;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.event.SelectionEvent.SelectionNotifier;
import com.vaadin.event.SortEvent;
import com.vaadin.event.SortEvent.SortListener;
import com.vaadin.event.SortEvent.SortNotifier;
import com.vaadin.hummingbird.kernel.DomEventListener;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.server.EncodeResult;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.JsonCodec;
import com.vaadin.server.KeyMapper;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.grid.EditorClientRpc;
import com.vaadin.shared.ui.grid.EditorServerRpc;
import com.vaadin.shared.ui.grid.GridClientRpc;
import com.vaadin.shared.ui.grid.GridServerRpc;
import com.vaadin.shared.ui.grid.GridState;
import com.vaadin.shared.ui.grid.GridStaticCellType;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.shared.ui.grid.ScrollDestination;
import com.vaadin.shared.util.SharedUtil;
import com.vaadin.ui.AbstractFocusable;
import com.vaadin.ui.Component;
import com.vaadin.ui.ConnectorTracker;
import com.vaadin.ui.Field;
import com.vaadin.ui.GridJSDataProvider;
import com.vaadin.ui.SelectiveRenderer;
import com.vaadin.ui.renderers.Renderer;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * A grid component for displaying tabular data.
 * <p>
 * Grid is always bound to a {@link Container.Indexed}, but is not a
 * {@code Container} of any kind in of itself. The contents of the given
 * Container is displayed with the help of {@link Renderer Renderers}.
 *
 * <h3 id="grid-headers-and-footers">Headers and Footers</h3>
 * <p>
 *
 *
 * <h3 id="grid-converters-and-renderers">Converters and Renderers</h3>
 * <p>
 * Each column has its own {@link Renderer} that displays data into something
 * that can be displayed in the browser. That data is first converted with a
 * {@link com.vaadin.data.util.converter.Converter Converter} into something
 * that the Renderer can process. This can also be an implicit step - if a
 * column has a simple data type, like a String, no explicit assignment is
 * needed.
 * <p>
 * Usually a renderer takes some kind of object, and converts it into a
 * HTML-formatted string.
 * <p>
 * <code><pre>
 * Grid grid = new Grid(myContainer);
 * Column column = grid.getColumn(STRING_DATE_PROPERTY);
 * column.setConverter(new StringToDateConverter());
 * column.setRenderer(new MyColorfulDateRenderer());
 * </pre></code>
 *
 * <h3 id="grid-lazyloading">Lazy Loading</h3>
 * <p>
 * The data is accessed as it is needed by Grid and not any sooner. In other
 * words, if the given Container is huge, but only the first few rows are
 * displayed to the user, only those (and a few more, for caching purposes) are
 * accessed.
 *
 * <h3 id="grid-selection-modes-and-models">Selection Modes and Models</h3>
 * <p>
 * Grid supports three selection <em>{@link SelectionMode modes}</em> (single,
 * multi, none), and comes bundled with one
 * <em>{@link SelectionModel model}</em> for each of the modes. The distinction
 * between a selection mode and selection model is as follows: a <em>mode</em>
 * essentially says whether you can have one, many or no rows selected. The
 * model, however, has the behavioral details of each. A single selection model
 * may require that the user deselects one row before selecting another one. A
 * variant of a multiselect might have a configurable maximum of rows that may
 * be selected. And so on.
 * <p>
 * <code><pre>
 * Grid grid = new Grid(myContainer);
 *
 * // uses the bundled SingleSelectionModel class
 * grid.setSelectionMode(SelectionMode.SINGLE);
 *
 * // changes the behavior to a custom selection model
 * grid.setSelectionModel(new MyTwoSelectionModel());
 * </pre></code>
 *
 * @since 7.4
 * @author Vaadin Ltd
 */
@Tag("vaadin-grid")
@Bower("vaadin-grid")
// Optimizations to load dependencies quicker
@JavaScript("context://bower_components/vaadin-grid/vaadin-grid.min.js")
@com.vaadin.annotations.HTML("context://bower_components/polymer/polymer.html")
@Implemented("Still missing support for most features")
public class Grid extends AbstractFocusable implements SelectionNotifier,
        SortNotifier, SelectiveRenderer, ItemClickNotifier {

    public static final Map<String, SortDirection> sortDirections = new HashMap<>();

    static {
        sortDirections.put("asc", SortDirection.ASCENDING);
        sortDirections.put("desc", SortDirection.DESCENDING);
    }

    /**
     * The data source attached to the grid
     */
    private Container.Indexed datasource;

    /**
     * Property id to column instance mapping
     */
    // FIXME
    public final Map<Object, Column> columns = new HashMap<Object, Column>();

    /**
     * Key generator for column server-to-client communication
     */
    final KeyMapper<Object> columnKeys = new KeyMapper<Object>();

    /**
     * The current sort order
     */
    private final List<SortOrder> sortOrder = new ArrayList<SortOrder>();

    /**
     * Property listener for listening to changes in data source properties.
     */
    private final PropertySetChangeListener propertyListener = new PropertySetChangeListener() {

        @Override
        public void containerPropertySetChange(PropertySetChangeEvent event) {
            Collection<?> properties = new HashSet<Object>(
                    event.getContainer().getContainerPropertyIds());

            // Find columns that need to be removed.
            List<Column> removedColumns = new LinkedList<Column>();
            for (Object propertyId : columns.keySet()) {
                if (!properties.contains(propertyId)) {
                    removedColumns.add(getColumn(propertyId));
                }
            }

            // Actually remove columns.
            for (Column column : removedColumns) {
                Object propertyId = column.getPropertyId();
                internalRemoveColumn(propertyId);
                columnKeys.remove(propertyId);
            }
            // datasourceExtension.columnsRemoved(removedColumns);

            // Add new columns
            List<Column> addedColumns = new LinkedList<Column>();
            for (Object propertyId : properties) {
                if (!columns.containsKey(propertyId)) {
                    addedColumns.add(appendColumn(propertyId));
                }
            }
            // datasourceExtension.columnsAdded(addedColumns);

            if (getFrozenColumnCount() > columns.size()) {
                setFrozenColumnCount(columns.size());
            }

            // Unset sortable for non-sortable columns.
            if (datasource instanceof Sortable) {
                Collection<?> sortables = ((Sortable) datasource)
                        .getSortableContainerPropertyIds();
                for (Object propertyId : columns.keySet()) {
                    Column column = columns.get(propertyId);
                    if (!sortables.contains(propertyId)
                            && column.isSortable()) {
                        column.setSortable(false);
                    }
                }
            }
        }
    };

    private final ItemSetChangeListener editorClosingItemSetListener = new ItemSetChangeListener() {
        @Override
        public void containerItemSetChange(ItemSetChangeEvent event) {
            cancelEditor();
        }
    };

    private GridJSDataProvider dataProvider;

    /**
     * The selection model that is currently in use. Never <code>null</code>
     * after the constructor has been run.
     */
    private SelectionModel selectionModel;

    /**
     * Used to know whether selection change events originate from the server or
     * the client so the selection change handler knows whether the changes
     * should be sent to the client.
     */
    private boolean applyingSelectionFromClient;

    private final Header header = new Header(this);
    private final Footer footer = new Footer(this);

    private Object editedItemId = null;
    private boolean editorActive = false;
    private FieldGroup editorFieldGroup = new CustomFieldGroup(this);

    CellStyleGenerator cellStyleGenerator;
    RowStyleGenerator rowStyleGenerator;

    CellDescriptionGenerator cellDescriptionGenerator;
    RowDescriptionGenerator rowDescriptionGenerator;

    /**
     * <code>true</code> if Grid is using the internal IndexedContainer created
     * in Grid() constructor, or <code>false</code> if the user has set their
     * own Container.
     *
     * @see #setContainerDataSource(Indexed)
     * @see #Grid()
     */
    private boolean defaultContainer = true;

    private EditorErrorHandler editorErrorHandler = new DefaultEditorErrorHandler(
            this);

    private final DomEventListener selectionDomListener = new DomEventListener() {
        @Override
        public void handleEvent(JsonObject eventData) {
            JsonArray selectedIndices = eventData.getArray("selection");
            Indexed container = getContainerDataSource();
            SelectionModel selectionModel = getSelectionModel();
            if (selectionModel instanceof SelectionModel.Single) {
                SelectionModel.Single ssm = (SelectionModel.Single) selectionModel;
                switch (selectedIndices.length()) {
                case 0:
                    ssm.reset();
                    break;
                case 1:
                    Object selectedItemId = container
                            .getIdByIndex((int) selectedIndices.getNumber(0));
                    ssm.select(selectedItemId);
                    break;
                default:
                    throw new RuntimeException(
                            "Got multiple selected values for a single selection model");

                }
            } else if (selectionModel instanceof SelectionModel.Multi) {
                SelectionModel.Multi msm = (SelectionModel.Multi) selectionModel;
                List<Object> itemids = new ArrayList<>();
                for (int i = 0; i < selectedIndices.length(); i++) {
                    itemids.add(container
                            .getIdByIndex((int) selectedIndices.getNumber(i)));
                }
                msm.select(itemids);
            } else {
                throw new RuntimeException("Unsupported selection model");
            }
        }
    };

    /**
     * Creates a new Grid with a new {@link IndexedContainer} as the data
     * source.
     */
    public Grid() {
        this(null, null);
    }

    /**
     * Creates a new Grid using the given data source.
     *
     * @param dataSource
     *            the indexed container to use as a data source
     */
    public Grid(final Container.Indexed dataSource) {
        this(null, dataSource);
    }

    /**
     * Creates a new Grid with the given caption and a new
     * {@link IndexedContainer} data source.
     *
     * @param caption
     *            the caption of the grid
     */
    public Grid(String caption) {
        this(caption, null);
    }

    /**
     * Creates a new Grid with the given caption and data source. If the data
     * source is null, a new {@link IndexedContainer} will be used.
     *
     * @param caption
     *            the caption of the grid
     * @param dataSource
     *            the indexed container to use as a data source
     */
    public Grid(String caption, Container.Indexed dataSource) {
        if (dataSource == null) {
            internalSetContainerDataSource(new IndexedContainer());
        } else {
            setContainerDataSource(dataSource);
        }
        setCaption(caption);
        initGrid();
    }

    /**
     * Grid initial setup
     */
    private void initGrid() {
        setSelectionMode(getDefaultSelectionMode());

        registerRpc(new GridServerRpc() {

            @Override
            public void sort(String[] columnIds, SortDirection[] directions,
                    boolean userOriginated) {
                assert columnIds.length == directions.length;

                List<SortOrder> order = new ArrayList<SortOrder>(
                        columnIds.length);
                for (int i = 0; i < columnIds.length; i++) {
                    Object propertyId = getPropertyIdByColumnId(columnIds[i]);
                    order.add(new SortOrder(propertyId, directions[i]));
                }
                setSortOrder(order, userOriginated);
                if (!order.equals(getSortOrder())) {
                    /*
                     * Actual sort order is not what the client expects. Make
                     * sure the client gets a state change event by clearing the
                     * diffstate and marking as dirty
                     */
                    ConnectorTracker connectorTracker = getUI()
                            .getConnectorTracker();
                    JsonObject diffState = connectorTracker
                            .getDiffState(Grid.this);
                    diffState.remove("sortColumns");
                    diffState.remove("sortDirs");
                    markAsDirty();
                }
            }

            // @Override
            // public void itemClick(String rowKey, String columnId,
            // MouseEventDetails details) {
            // Object itemId = getKeyMapper().get(rowKey);
            // Item item = datasource.getItem(itemId);
            // Object propertyId = getPropertyIdByColumnId(columnId);
            // fireEvent(new ItemClickEvent(Grid.this, item, itemId,
            // propertyId, details));
            // }

            @Override
            public void columnsReordered(List<String> newColumnOrder,
                    List<String> oldColumnOrder) {
                // FIXME
                // final String diffStateKey = "columnOrder";
                // ConnectorTracker connectorTracker = getUI()
                // .getConnectorTracker();
                // JsonObject diffState =
                // connectorTracker.getDiffState(Grid.this);
                // discard the change if the columns have been reordered from
                // the server side, as the server side is always right
                // if (getState(false).columnOrder.equals(oldColumnOrder)) {
                // // Don't mark as dirty since client has the state already
                // getState(false).columnOrder = newColumnOrder;
                // // write changes to diffState so that possible reverting the
                // // column order is sent to client
                // assert diffState
                // .hasKey(diffStateKey) : "Field name has changed";
                // Type type = null;
                // try {
                // type = (getState(false).getClass()
                // .getDeclaredField(diffStateKey)
                // .getGenericType());
                // } catch (NoSuchFieldException e) {
                // e.printStackTrace();
                // } catch (SecurityException e) {
                // e.printStackTrace();
                // }
                // EncodeResult encodeResult = JsonCodec.encode(
                // getState(false).columnOrder, diffState, type,
                // connectorTracker);
                //
                // diffState.put(diffStateKey, encodeResult.getEncodedValue());
                // fireColumnReorderEvent(true);
                // } else {
                // // make sure the client is reverted to the order that the
                // // server thinks it is
                // diffState.remove(diffStateKey);
                // markAsDirty();
                // }
            }

            @Override
            public void columnVisibilityChanged(String id, boolean hidden,
                    boolean userOriginated) {
                final Column column = getColumnByColumnId(id);
                // final GridColumnState columnState = column.getState();

                if (column.isHidden() != hidden) {
                    column.setHidden(hidden);

                    final String diffStateKey = "columns";
                    ConnectorTracker connectorTracker = getUI()
                            .getConnectorTracker();
                    JsonObject diffState = connectorTracker
                            .getDiffState(Grid.this);

                    assert diffState
                            .hasKey(diffStateKey) : "Field name has changed";
                    Type type = null;
                    try {
                        type = (getState(false).getClass()
                                .getDeclaredField(diffStateKey)
                                .getGenericType());
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    EncodeResult encodeResult = JsonCodec.encode(
                            getState(false).columns, diffState, type,
                            connectorTracker);

                    diffState.put(diffStateKey, encodeResult.getEncodedValue());

                    fireColumnVisibilityChangeEvent(column, hidden,
                            userOriginated);
                }
            }

            // @Override
            // public void editorOpen(String rowKey) {
            // fireEvent(new EditorOpenEvent(Grid.this,
            // getKeyMapper().get(rowKey)));
            // }
            //
            // @Override
            // public void editorMove(String rowKey) {
            // fireEvent(new EditorMoveEvent(Grid.this,
            // getKeyMapper().get(rowKey)));
            // }
            //
            // @Override
            // public void editorClose(String rowKey) {
            // fireEvent(new EditorCloseEvent(Grid.this,
            // getKeyMapper().get(rowKey)));
            // }
        });

        registerRpc(new EditorServerRpc() {

            @Override
            public void bind(int rowIndex) {
                try {
                    Object id = getContainerDataSource().getIdByIndex(rowIndex);

                    final boolean opening = editedItemId == null;

                    final boolean moving = !opening && !editedItemId.equals(id);

                    final boolean allowMove = !isEditorBuffered()
                            && getEditorFieldGroup().isValid();

                    if (opening || !moving || allowMove) {
                        doBind(id);
                    } else {
                        failBind(null);
                    }
                } catch (Exception e) {
                    failBind(e);
                }
            }

            private void doBind(Object id) {
                editedItemId = id;
                doEditItem();
                getEditorRpc().confirmBind(true);
            }

            private void failBind(Exception e) {
                if (e != null) {
                    handleError(e);
                }
                getEditorRpc().confirmBind(false);
            }

            @Override
            public void cancel(int rowIndex) {
                try {
                    // For future proofing even though cannot currently fail
                    doCancelEditor();
                } catch (Exception e) {
                    handleError(e);
                }
            }

            @Override
            public void save(int rowIndex) {
                List<String> errorColumnIds = null;
                String errorMessage = null;
                boolean success = false;
                try {
                    saveEditor();
                    success = true;
                } catch (CommitException e) {
                    try {
                        CommitErrorEvent event = new CommitErrorEvent(Grid.this,
                                e);
                        getEditorErrorHandler().commitError(event);

                        errorMessage = event.getUserErrorMessage();

                        errorColumnIds = new ArrayList<String>();
                        for (Column column : event.getErrorColumns()) {
                            // FIXME
                            // errorColumnIds.add(column.state.id);
                        }
                    } catch (Exception ee) {
                        // A badly written error handler can throw an exception,
                        // which would lock up the Grid
                        handleError(ee);
                    }
                } catch (Exception e) {
                    handleError(e);
                }
                getEditorRpc().confirmSave(success, errorMessage,
                        errorColumnIds);
            }

            private void handleError(Exception e) {
                com.vaadin.server.ErrorEvent.findErrorHandler(Grid.this)
                        .error(new ConnectorErrorEvent(Grid.this, e));
            }
        });

    }

    @Override
    protected void init() {
        super.init();

        // Always listen to selection events so selection on server is up to
        // date
        getElement().addEventData("hSelect", "selection");
        getElement().addEventListener("hSelect", selectionDomListener);

        dataProvider = new GridJSDataProvider(this);
        dataProvider.setContainer(getContainerDataSource());
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        try {
            header.sanityCheck();
            footer.sanityCheck();
        } catch (Exception e) {
            e.printStackTrace();
            setComponentError(new ErrorMessage() {

                @Override
                public ErrorLevel getErrorLevel() {
                    return ErrorLevel.CRITICAL;
                }

                @Override
                public String getFormattedHtmlMessage() {
                    return "Incorrectly merged cells";
                }

            });
        }

        super.beforeClientResponse(initial);
    }

    /**
     * Sets the grid data source.
     *
     * @param container
     *            The container data source. Cannot be null.
     * @throws IllegalArgumentException
     *             if the data source is null
     */
    public void setContainerDataSource(Container.Indexed container) {
        defaultContainer = false;
        internalSetContainerDataSource(container);
    }

    private void internalSetContainerDataSource(Container.Indexed container) {
        if (container == null) {
            throw new IllegalArgumentException(
                    "Cannot set the datasource to null");
        }
        if (datasource == container) {
            return;
        }

        // Remove old listeners
        if (datasource instanceof PropertySetChangeNotifier) {
            ((PropertySetChangeNotifier) datasource)
                    .removePropertySetChangeListener(propertyListener);
        }

        resetEditor();
        // Remove old container to allow data provider to remove listeners while
        // the old properties still exist in this grid
        if (dataProvider != null) {
            dataProvider.setContainer(null);
        }

        datasource = container;

        //
        // Adjust sort order
        //

        if (container instanceof Container.Sortable) {

            // If the container is sortable, go through the current sort order
            // and match each item to the sortable properties of the new
            // container. If the new container does not support an item in the
            // current sort order, that item is removed from the current sort
            // order list.
            Collection<?> sortableProps = ((Container.Sortable) getContainerDataSource())
                    .getSortableContainerPropertyIds();

            Iterator<SortOrder> i = sortOrder.iterator();
            while (i.hasNext()) {
                if (!sortableProps.contains(i.next().getPropertyId())) {
                    i.remove();
                }
            }

            sort(false);
        } else {
            // Clear sorting order. Don't sort.
            sortOrder.clear();
        }

        if (dataProvider != null) {
            dataProvider.setContainer(container);
        }

        /*
         * selectionModel == null when the invocation comes from the
         * constructor.
         */
        if (selectionModel != null) {
            selectionModel.reset();
        }

        // Listen to changes in properties and remove columns if needed
        if (datasource instanceof PropertySetChangeNotifier) {
            ((PropertySetChangeNotifier) datasource)
                    .addPropertySetChangeListener(propertyListener);
        }

        /*
         * activeRowHandler will be updated by the client-side request that
         * occurs on container change - no need to actively re-insert any
         * ValueChangeListeners at this point.
         */

        setFrozenColumnCount(0);

        if (columns.isEmpty()) {
            // Add columns
            for (Object propertyId : datasource.getContainerPropertyIds()) {
                Column column = appendColumn(propertyId);

                // Initial sorting is defined by container
                if (datasource instanceof Sortable) {
                    column.setSortable(((Sortable) datasource)
                            .getSortableContainerPropertyIds()
                            .contains(propertyId));
                } else {
                    column.setSortable(false);
                }
            }
        } else {
            Collection<?> properties = datasource.getContainerPropertyIds();
            for (Object property : columns.keySet()) {
                if (!properties.contains(property)) {
                    throw new IllegalStateException(
                            "Found at least one column in Grid that does not exist in the given container: "
                                    + property + " with the header \""
                                    + getColumn(property).getHeaderCaption()
                                    + "\"");
                }

                if (!(datasource instanceof Sortable)
                        || !((Sortable) datasource)
                                .getSortableContainerPropertyIds()
                                .contains(property)) {
                    columns.get(property).setSortable(false);
                }
            }
        }

    }

    /**
     * Returns the grid data source.
     *
     * @return the container data source of the grid
     */
    public Container.Indexed getContainerDataSource() {
        return datasource;
    }

    /**
     * Returns a column based on the property id
     *
     * @param propertyId
     *            the property id of the column
     * @return the column or <code>null</code> if not found
     */
    public Column getColumn(Object propertyId) {
        return columns.get(propertyId);
    }

    /**
     * Returns a copy of currently configures columns in their current visual
     * order in this Grid.
     *
     * @return unmodifiable copy of current columns in visual order
     */
    public List<Column> getColumns() {
        List<Column> columns = new ArrayList<>();
        for (StateNode columnStateNode : getColumnStateNodes()) {
            columns.add(columnStateNode.get(Column.class, Column.class));
        }

        return Collections.unmodifiableList(columns);
    }

    /**
     * Adds a new Column to Grid. Also adds the property to container with data
     * type String, if property for column does not exist in it. Default value
     * for the new property is an empty String.
     * <p>
     * Note that adding a new property is only done for the default container
     * that Grid sets up with the default constructor.
     *
     * @param propertyId
     *            the property id of the new column
     * @return the new column
     *
     * @throws IllegalStateException
     *             if column for given property already exists in this grid
     */

    public Column addColumn(Object propertyId) throws IllegalStateException {
        if (datasource.getContainerPropertyIds().contains(propertyId)
                && !columns.containsKey(propertyId)) {
            appendColumn(propertyId);
        } else if (defaultContainer) {
            addColumnProperty(propertyId, String.class, "");
        } else {
            if (columns.containsKey(propertyId)) {
                throw new IllegalStateException(
                        "A column for property id '" + propertyId.toString()
                                + "' already exists in this grid");
            } else {
                throw new IllegalStateException(
                        "Property id '" + propertyId.toString()
                                + "' does not exist in the container");
            }
        }

        // Inform the data provider of this new column.
        Column column = getColumn(propertyId);
        List<Column> addedColumns = new ArrayList<Column>();
        addedColumns.add(column);
        // datasourceExtension.columnsAdded(addedColumns);

        return column;
    }

    /**
     * Adds a new Column to Grid. This function makes sure that the property
     * with the given id and data type exists in the container. If property does
     * not exists, it will be created.
     * <p>
     * Default value for the new property is 0 if type is Integer, Double and
     * Float. If type is String, default value is an empty string. For all other
     * types the default value is null.
     * <p>
     * Note that adding a new property is only done for the default container
     * that Grid sets up with the default constructor.
     *
     * @param propertyId
     *            the property id of the new column
     * @param type
     *            the data type for the new property
     * @return the new column
     *
     * @throws IllegalStateException
     *             if column for given property already exists in this grid or
     *             property already exists in the container with wrong type
     */
    public Column addColumn(Object propertyId, Class<?> type) {
        addColumnProperty(propertyId, type, null);
        return getColumn(propertyId);
    }

    protected void addColumnProperty(Object propertyId, Class<?> type,
            Object defaultValue) throws IllegalStateException {
        if (!defaultContainer) {
            throw new IllegalStateException(
                    "Container for this Grid is not a default container from Grid() constructor");
        }

        if (!columns.containsKey(propertyId)) {
            if (!datasource.getContainerPropertyIds().contains(propertyId)) {
                datasource.addContainerProperty(propertyId, type, defaultValue);
            } else {
                Property<?> containerProperty = datasource.getContainerProperty(
                        datasource.firstItemId(), propertyId);
                if (containerProperty.getType() == type) {
                    appendColumn(propertyId);
                } else {
                    throw new IllegalStateException(
                            "DataSource already has the given property "
                                    + propertyId + " with a different type");
                }
            }
        } else {
            throw new IllegalStateException(
                    "Grid already has a column for property " + propertyId);
        }
    }

    /**
     * Removes all columns from this Grid.
     */
    public void removeAllColumns() {
        List<Column> removed = new ArrayList<Column>(columns.values());
        Set<Object> properties = new HashSet<Object>(columns.keySet());
        for (Object propertyId : properties) {
            removeColumn(propertyId);
        }
        // datasourceExtension.columnsRemoved(removed);
    }

    /**
     * Used internally by the {@link Grid} to get a {@link Column} by
     * referencing its generated state id. Also used by {@link Column} to verify
     * if it has been detached from the {@link Grid}.
     *
     * @param columnId
     *            the client id generated for the column when the column is
     *            added to the grid
     * @return the column with the id or <code>null</code> if not found
     */
    Column getColumnByColumnId(String columnId) {
        Object propertyId = getPropertyIdByColumnId(columnId);
        return getColumn(propertyId);
    }

    /**
     * Used internally by the {@link Grid} to get a property id by referencing
     * the columns generated state id.
     *
     * @param columnId
     *            The state id of the column
     * @return The column instance or null if not found
     */
    public Object getPropertyIdByColumnId(String columnId) {
        return columnKeys.get(columnId);
    }

    public String getColumnIdByPropertyId(Object propertyId) {
        return columnKeys.key(propertyId);
    }

    /**
     * Returns whether column reordering is allowed. Default value is
     * <code>false</code>.
     *
     * @since 7.5.0
     * @return true if reordering is allowed
     */
    public boolean isColumnReorderingAllowed() {
        return getState(false).columnReorderingAllowed;
    }

    /**
     * Sets whether or not column reordering is allowed. Default value is
     * <code>false</code>.
     *
     * @since 7.5.0
     * @param columnReorderingAllowed
     *            specifies whether column reordering is allowed
     */
    public void setColumnReorderingAllowed(boolean columnReorderingAllowed) {
        if (isColumnReorderingAllowed() != columnReorderingAllowed) {
            getState().columnReorderingAllowed = columnReorderingAllowed;
        }
    }

    @Override
    protected GridState getState() {
        return (GridState) super.getState();
    }

    @Override
    public GridState getState(boolean markAsDirty) {
        return (GridState) super.getState(markAsDirty);
    }

    /**
     * Creates a new column based on a property id and appends it as the last
     * column.
     *
     * @param datasourcePropertyId
     *            The property id of a property in the datasource
     */
    private Column appendColumn(Object datasourcePropertyId) {
        if (datasourcePropertyId == null) {
            throw new IllegalArgumentException("Property id cannot be null");
        }
        assert datasource.getContainerPropertyIds().contains(
                datasourcePropertyId) : "Datasource should contain the property id";

        StateNode columnState = StateNode.create();
        getColumnStateNodes().add(columnState);
        Column column = new Column(this, columnState, datasourcePropertyId);
        columnState.put(Column.class, column);
        columns.put(datasourcePropertyId, column);

        // getState().columns.add(columnState);
        header.addColumn(datasourcePropertyId);
        footer.addColumn(datasourcePropertyId);

        String humanFriendlyPropertyId = SharedUtil.propertyIdToHumanFriendly(
                String.valueOf(datasourcePropertyId));
        column.setHeaderCaption(humanFriendlyPropertyId);

        if (datasource instanceof Sortable
                && ((Sortable) datasource).getSortableContainerPropertyIds()
                        .contains(datasourcePropertyId)) {
            column.setSortable(true);
        }

        return column;
    }

    @SuppressWarnings("unchecked")
    private List<StateNode> getColumnStateNodes() {
        return (List) getElement().getElementDataNode()
                .getMultiValued("columns");
    }

    /**
     * Removes a column from Grid based on a property id.
     *
     * @param propertyId
     *            The property id of column to be removed
     *
     * @throws IllegalArgumentException
     *             if there is no column for given property id in this grid
     */
    public void removeColumn(Object propertyId)
            throws IllegalArgumentException {
        if (!columns.keySet().contains(propertyId)) {
            throw new IllegalArgumentException(
                    "There is no column for given property id " + propertyId);
        }

        List<Column> removed = new ArrayList<Column>();
        removed.add(getColumn(propertyId));
        internalRemoveColumn(propertyId);
        // datasourceExtension.columnsRemoved(removed);
    }

    private void internalRemoveColumn(Object propertyId) {
        setEditorField(propertyId, null);
        header.removeColumn(propertyId);
        footer.removeColumn(propertyId);
        Column column = columns.remove(propertyId);
        getState().columns.remove(column.getState());
        getColumnStateNodes().remove(column.getState());
        // removeExtension(column.getRenderer());
    }

    /**
     * Sets the columns and their order for the grid. Current columns whose
     * property id is not in propertyIds are removed. Similarly, a column is
     * added for any property id in propertyIds that has no corresponding column
     * in this Grid.
     *
     * @since 7.5.0
     *
     * @param propertyIds
     *            properties in the desired column order
     */
    public void setColumns(Object... propertyIds) {
        Set<?> removePids = new HashSet<Object>(columns.keySet());
        removePids.removeAll(Arrays.asList(propertyIds));
        for (Object removePid : removePids) {
            removeColumn(removePid);
        }
        Set<?> addPids = new HashSet<Object>(Arrays.asList(propertyIds));
        addPids.removeAll(columns.keySet());
        for (Object propertyId : addPids) {
            addColumn(propertyId);
        }
        setColumnOrder(propertyIds);
    }

    /**
     * Sets a new column order for the grid. All columns which are not ordered
     * here will remain in the order they were before as the last columns of
     * grid.
     *
     * @param propertyIds
     *            properties in the order columns should be
     */
    public void setColumnOrder(Object... propertyIds) {
        List<StateNode> columnOrder = new ArrayList<>();
        for (Object propertyId : propertyIds) {
            if (columns.containsKey(propertyId)) {
                columnOrder.add(columns.get(propertyId).getState());
            } else {
                throw new IllegalArgumentException(
                        "Grid does not contain column for property "
                                + String.valueOf(propertyId));
            }
        }

        List<StateNode> columnStateNodes = getColumnStateNodes();
        for (int i = 0; i < columnOrder.size(); i++) {
            StateNode column = columnOrder.get(i);
            assert columnStateNodes.contains(column);

            if (columnStateNodes.get(i) != column) {
                columnStateNodes.remove(column);
                columnStateNodes.add(i, column);
            }
        }

        fireColumnReorderEvent(false);
    }

    /**
     * Sets the number of frozen columns in this grid. Setting the count to 0
     * means that no data columns will be frozen, but the built-in selection
     * checkbox column will still be frozen if it's in use. Setting the count to
     * -1 will also disable the selection column.
     * <p>
     * The default value is 0.
     *
     * @param numberOfColumns
     *            the number of columns that should be frozen
     *
     * @throws IllegalArgumentException
     *             if the column count is < 0 or > the number of visible columns
     */
    public void setFrozenColumnCount(int numberOfColumns) {
        if (numberOfColumns < -1 || numberOfColumns > columns.size()) {
            throw new IllegalArgumentException(
                    "count must be between -1 and the current number of columns ("
                            + columns.size() + "): " + numberOfColumns);
        }

        getElement().setAttribute("frozenColumns", numberOfColumns);
    }

    /**
     * Gets the number of frozen columns in this grid. 0 means that no data
     * columns will be frozen, but the built-in selection checkbox column will
     * still be frozen if it's in use. -1 means that not even the selection
     * column is frozen.
     * <p>
     * <em>NOTE:</em> this count includes {@link Column#isHidden() hidden
     * columns} in the count.
     *
     * @see #setFrozenColumnCount(int)
     *
     * @return the number of frozen columns
     */
    public int getFrozenColumnCount() {
        return getElement().getAttribute("frozenColumns", 0);
    }

    /**
     * Scrolls to a certain item, using {@link ScrollDestination#ANY}.
     * <p>
     * If the item has visible details, its size will also be taken into
     * account.
     *
     * @param itemId
     *            id of item to scroll to.
     * @throws IllegalArgumentException
     *             if the provided id is not recognized by the data source.
     */
    public void scrollTo(Object itemId) throws IllegalArgumentException {
        scrollTo(itemId, ScrollDestination.ANY);
    }

    /**
     * Scrolls to a certain item, using user-specified scroll destination.
     * <p>
     * If the item has visible details, its size will also be taken into
     * account.
     *
     * @param itemId
     *            id of item to scroll to.
     * @param destination
     *            value specifying desired position of scrolled-to row.
     * @throws IllegalArgumentException
     *             if the provided id is not recognized by the data source.
     */
    public void scrollTo(Object itemId, ScrollDestination destination)
            throws IllegalArgumentException {

        int row = datasource.indexOfId(itemId);

        if (row == -1) {
            throw new IllegalArgumentException(
                    "Item with specified ID does not exist in data source");
        }

        GridClientRpc clientRPC = getRpcProxy(GridClientRpc.class);
        clientRPC.scrollToRow(row, destination);
    }

    /**
     * Scrolls to the beginning of the first data row.
     */
    public void scrollToStart() {
        GridClientRpc clientRPC = getRpcProxy(GridClientRpc.class);
        clientRPC.scrollToStart();
    }

    /**
     * Scrolls to the end of the last data row.
     */
    public void scrollToEnd() {
        GridClientRpc clientRPC = getRpcProxy(GridClientRpc.class);
        clientRPC.scrollToEnd();
    }

    /**
     * Sets the number of rows that should be visible in Grid's body, while
     * {@link #getHeightMode()} is {@link HeightMode#ROW}.
     * <p>
     * If Grid is currently not in {@link HeightMode#ROW}, the given value is
     * remembered, and applied once the mode is applied.
     *
     * @param rows
     *            The height in terms of number of rows displayed in Grid's
     *            body. If Grid doesn't contain enough rows, white space is
     *            displayed instead. If <code>null</code> is given, then Grid's
     *            height is undefined
     * @throws IllegalArgumentException
     *             if {@code rows} is zero or less
     * @throws IllegalArgumentException
     *             if {@code rows} is {@link Double#isInfinite(double) infinite}
     * @throws IllegalArgumentException
     *             if {@code rows} is {@link Double#isNaN(double) NaN}
     */
    public void setHeightByRows(double rows) {
        if (rows <= 0.0d) {
            throw new IllegalArgumentException(
                    "More than zero rows must be shown.");
        } else if (Double.isInfinite(rows)) {
            throw new IllegalArgumentException(
                    "Grid doesn't support infinite heights");
        } else if (Double.isNaN(rows)) {
            throw new IllegalArgumentException("NaN is not a valid row count");
        }

        getElement().setAttribute("rows", rows);
    }

    /**
     * Gets the amount of rows in Grid's body that are shown, while
     * {@link #getHeightMode()} is {@link HeightMode#ROW}.
     *
     * @return the amount of rows that are being shown in Grid's body
     * @see #setHeightByRows(double)
     */
    public double getHeightByRows() {
        return getElement().getAttribute("rows", -1.0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <em>Note:</em> This method will change the widget's size in the browser
     * only if {@link #getHeightMode()} returns {@link HeightMode#CSS}.
     *
     * @see #setHeightMode(HeightMode)
     */
    @Override
    public void setHeight(float height, Unit unit) {
        super.setHeight(height, unit);
    }

    /**
     * Defines the mode in which the Grid widget's height is calculated.
     * <p>
     * If {@link HeightMode#CSS} is given, Grid will respect the values given
     * via a {@code setHeight}-method, and behave as a traditional Component.
     * <p>
     * If {@link HeightMode#ROW} is given, Grid will make sure that the body
     * will display as many rows as {@link #getHeightByRows()} defines.
     * <em>Note:</em> If headers/footers are inserted or removed, the widget
     * will resize itself to still display the required amount of rows in its
     * body. It also takes the horizontal scrollbar into account.
     *
     * @param heightMode
     *            the mode in to which Grid should be set
     */
    public void setHeightMode(HeightMode heightMode) {
        /*
         * This method is a workaround for the fact that Vaadin re-applies
         * widget dimensions (height/width) on each state change event. The
         * original design was to have setHeight an setHeightByRow be equals,
         * and whichever was called the latest was considered in effect.
         *
         * But, because of Vaadin always calling setHeight on the widget, this
         * approach doesn't work.
         */

        getState().heightMode = heightMode;
    }

    /**
     * Returns the current {@link HeightMode} the Grid is in.
     * <p>
     * Defaults to {@link HeightMode#CSS}.
     *
     * @return the current HeightMode
     */
    public HeightMode getHeightMode() {
        return getState(false).heightMode;
    }

    /* Selection related methods: */

    /**
     * Takes a new {@link SelectionModel} into use.
     * <p>
     * The SelectionModel that is previously in use will have all its items
     * deselected.
     * <p>
     * If the given SelectionModel is already in use, this method does nothing.
     *
     * @param selectionModel
     *            the new SelectionModel to use
     * @throws IllegalArgumentException
     *             if {@code selectionModel} is <code>null</code>
     */
    public void setSelectionModel(SelectionModel selectionModel)
            throws IllegalArgumentException {
        if (selectionModel == null) {
            throw new IllegalArgumentException(
                    "Selection model may not be null");
        }

        if (this.selectionModel != selectionModel) {
            // this.selectionModel is null on init
            if (this.selectionModel != null) {
                // this.selectionModel.remove();
            }

            this.selectionModel = selectionModel;
            selectionModel.setGrid(this);
        }
    }

    /**
     * Returns the currently used {@link SelectionModel}.
     *
     * @return the currently used SelectionModel
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Sets the Grid's selection mode.
     * <p>
     * Grid supports three selection modes: multiselect, single select and no
     * selection, and this is a convenience method for choosing between one of
     * them.
     * <p>
     * Technically, this method is a shortcut that can be used instead of
     * calling {@code setSelectionModel} with a specific SelectionModel
     * instance. Grid comes with three built-in SelectionModel classes, and the
     * {@link SelectionMode} enum represents each of them.
     * <p>
     * Essentially, the two following method calls are equivalent:
     * <p>
     * <code><pre>
     * grid.setSelectionMode(SelectionMode.MULTI);
     * grid.setSelectionModel(new MultiSelectionMode());
     * </pre></code>
     *
     *
     * @param selectionMode
     *            the selection mode to switch to
     * @return The {@link SelectionModel} instance that was taken into use
     * @throws IllegalArgumentException
     *             if {@code selectionMode} is <code>null</code>
     * @see SelectionModel
     */
    public SelectionModel setSelectionMode(final SelectionMode selectionMode)
            throws IllegalArgumentException {
        if (selectionMode == null) {
            throw new IllegalArgumentException(
                    "selection mode may not be null");
        }
        final SelectionModel newSelectionModel = selectionMode.createModel();
        setSelectionModel(newSelectionModel);
        return newSelectionModel;
    }

    /**
     * Checks whether an item is selected or not.
     *
     * @param itemId
     *            the item id to check for
     * @return <code>true</code> iff the item is selected
     */
    // keep this javadoc in sync with SelectionModel.isSelected
    public boolean isSelected(Object itemId) {
        return selectionModel.isSelected(itemId);
    }

    /**
     * Returns a collection of all the currently selected itemIds.
     * <p>
     * This method is a shorthand that delegates to the
     * {@link #getSelectionModel() selection model}.
     *
     * @return a collection of all the currently selected itemIds
     */
    // keep this javadoc in sync with SelectionModel.getSelectedRows
    public Collection<Object> getSelectedRows() {
        return getSelectionModel().getSelectedRows();
    }

    /**
     * Gets the item id of the currently selected item.
     * <p>
     * This method is a shorthand that delegates to the
     * {@link #getSelectionModel() selection model}. Only
     * {@link SelectionModel.Single} is supported.
     *
     * @return the item id of the currently selected item, or <code>null</code>
     *         if nothing is selected
     * @throws IllegalStateException
     *             if the selection model does not implement
     *             {@code SelectionModel.Single}
     */
    // keep this javadoc in sync with SelectionModel.Single.getSelectedRow
    public Object getSelectedRow() throws IllegalStateException {
        if (selectionModel instanceof SelectionModel.Single) {
            return ((SelectionModel.Single) selectionModel).getSelectedRow();
        } else if (selectionModel instanceof SelectionModel.Multi) {
            throw new IllegalStateException("Cannot get unique selected row: "
                    + "Grid is in multiselect mode "
                    + "(the current selection model is "
                    + selectionModel.getClass().getName() + ").");
        } else if (selectionModel instanceof SelectionModel.None) {
            throw new IllegalStateException(
                    "Cannot get selected row: " + "Grid selection is disabled "
                            + "(the current selection model is "
                            + selectionModel.getClass().getName() + ").");
        } else {
            throw new IllegalStateException("Cannot get selected row: "
                    + "Grid selection model does not implement "
                    + SelectionModel.Single.class.getName() + " or "
                    + SelectionModel.Multi.class.getName()
                    + "(the current model is "
                    + selectionModel.getClass().getName() + ").");
        }
    }

    /**
     * Marks an item as selected.
     * <p>
     * This method is a shorthand that delegates to the
     * {@link #getSelectionModel() selection model}. Only
     * {@link SelectionModel.Single} and {@link SelectionModel.Multi} are
     * supported.
     *
     * @param itemId
     *            the itemId to mark as selected
     * @return <code>true</code> if the selection state changed,
     *         <code>false</code> if the itemId already was selected
     * @throws IllegalArgumentException
     *             if the {@code itemId} doesn't exist in the currently active
     *             Container
     * @throws IllegalStateException
     *             if the selection was illegal. One such reason might be that
     *             the implementation already had an item selected, and that
     *             needs to be explicitly deselected before re-selecting
     *             something.
     * @throws IllegalStateException
     *             if the selection model does not implement
     *             {@code SelectionModel.Single} or {@code SelectionModel.Multi}
     */
    // keep this javadoc in sync with SelectionModel.Single.select
    public boolean select(Object itemId)
            throws IllegalArgumentException, IllegalStateException {
        if (selectionModel instanceof SelectionModel.Single) {
            return ((SelectionModel.Single) selectionModel).select(itemId);
        } else if (selectionModel instanceof SelectionModel.Multi) {
            return ((SelectionModel.Multi) selectionModel).select(itemId);
        } else if (selectionModel instanceof SelectionModel.None) {
            throw new IllegalStateException("Cannot select row '" + itemId
                    + "': Grid selection is disabled "
                    + "(the current selection model is "
                    + selectionModel.getClass().getName() + ").");
        } else {
            throw new IllegalStateException("Cannot select row '" + itemId
                    + "': Grid selection model does not implement "
                    + SelectionModel.Single.class.getName() + " or "
                    + SelectionModel.Multi.class.getName()
                    + "(the current model is "
                    + selectionModel.getClass().getName() + ").");
        }
    }

    /**
     * Marks an item as unselected.
     * <p>
     * This method is a shorthand that delegates to the
     * {@link #getSelectionModel() selection model}. Only
     * {@link SelectionModel.Single} and {@link SelectionModel.Multi} are
     * supported.
     *
     * @param itemId
     *            the itemId to remove from being selected
     * @return <code>true</code> if the selection state changed,
     *         <code>false</code> if the itemId was already selected
     * @throws IllegalArgumentException
     *             if the {@code itemId} doesn't exist in the currently active
     *             Container
     * @throws IllegalStateException
     *             if the deselection was illegal. One such reason might be that
     *             the implementation requires one or more items to be selected
     *             at all times.
     * @throws IllegalStateException
     *             if the selection model does not implement
     *             {@code SelectionModel.Single} or {code SelectionModel.Multi}
     */
    // keep this javadoc in sync with SelectionModel.Single.deselect
    public boolean deselect(Object itemId) throws IllegalStateException {
        if (selectionModel instanceof SelectionModel.Single) {
            if (isSelected(itemId)) {
                return ((SelectionModel.Single) selectionModel).select(null);
            }
            return false;
        } else if (selectionModel instanceof SelectionModel.Multi) {
            return ((SelectionModel.Multi) selectionModel).deselect(itemId);
        } else if (selectionModel instanceof SelectionModel.None) {
            throw new IllegalStateException("Cannot deselect row '" + itemId
                    + "': Grid selection is disabled "
                    + "(the current selection model is "
                    + selectionModel.getClass().getName() + ").");
        } else {
            throw new IllegalStateException("Cannot deselect row '" + itemId
                    + "': Grid selection model does not implement "
                    + SelectionModel.Single.class.getName() + " or "
                    + SelectionModel.Multi.class.getName()
                    + "(the current model is "
                    + selectionModel.getClass().getName() + ").");
        }
    }

    /**
     * Fires a selection change event.
     * <p>
     * <strong>Note:</strong> This is not a method that should be called by
     * application logic. This method is publicly accessible only so that
     * {@link SelectionModel SelectionModels} would be able to inform Grid of
     * these events.
     *
     * @param newSelection
     *            the selection that was added by this event
     * @param oldSelection
     *            the selection that was removed by this event
     */
    public void fireSelectionEvent(Collection<Object> oldSelection,
            Collection<Object> newSelection) {
        fireEvent(new SelectionEvent(this, oldSelection, newSelection));
    }

    @Override
    public void addSelectionListener(SelectionListener listener) {
        addListener(SelectionEvent.class, listener);
    }

    @Override
    public void removeSelectionListener(SelectionListener listener) {
        removeListener(SelectionEvent.class, listener);
    }

    private void fireColumnReorderEvent(boolean userOriginated) {
        fireEvent(new ColumnReorderEvent(this, userOriginated));
    }

    /**
     * Registers a new column reorder listener.
     *
     * @since 7.5.0
     * @param listener
     *            the listener to register
     */
    public void addColumnReorderListener(ColumnReorderListener listener) {
        addListener(ColumnReorderEvent.class, listener);
    }

    /**
     * Removes a previously registered column reorder listener.
     *
     * @since 7.5.0
     * @param listener
     *            the listener to remove
     */
    public void removeColumnReorderListener(ColumnReorderListener listener) {
        removeListener(ColumnReorderEvent.class, listener);
    }

    // /**
    // * Gets the {@link
    // com.vaadin.data.GridJSDataProvider.DataProviderKeyMapper
    // * DataProviderKeyMapper} being used by the data source.
    // *
    // * @return the key mapper being used by the data source
    // */
    // KeyMapper<Object> getKeyMapper() {
    // return datasourceExtension.getKeyMapper();
    // }

    /**
     * Adds a renderer to this grid's connector hierarchy.
     *
     * @param renderer
     *            the renderer to add
     */
    void addRenderer(Renderer<?> renderer) {
        // addExtension(renderer);
    }

    /**
     * Sets the current sort order using the fluid Sort API. Read the
     * documentation for {@link Sort} for more information.
     * <p>
     * <em>Note:</em> Sorting by a property that has no column in Grid will hide
     * all possible sorting indicators.
     *
     * @param s
     *            a sort instance
     *
     * @throws IllegalStateException
     *             if container is not sortable (does not implement
     *             Container.Sortable)
     * @throws IllegalArgumentException
     *             if trying to sort by non-existing property
     */
    public void sort(Sort s) {
        setSortOrder(s.build());
    }

    /**
     * Sort this Grid in ascending order by a specified property.
     * <p>
     * <em>Note:</em> Sorting by a property that has no column in Grid will hide
     * all possible sorting indicators.
     *
     * @param propertyId
     *            a property ID
     *
     * @throws IllegalStateException
     *             if container is not sortable (does not implement
     *             Container.Sortable)
     * @throws IllegalArgumentException
     *             if trying to sort by non-existing property
     */
    public void sort(Object propertyId) {
        sort(propertyId, SortDirection.ASCENDING);
    }

    /**
     * Sort this Grid in user-specified {@link SortOrder} by a property.
     * <p>
     * <em>Note:</em> Sorting by a property that has no column in Grid will hide
     * all possible sorting indicators.
     *
     * @param propertyId
     *            a property ID
     * @param direction
     *            a sort order value (ascending/descending)
     *
     * @throws IllegalStateException
     *             if container is not sortable (does not implement
     *             Container.Sortable)
     * @throws IllegalArgumentException
     *             if trying to sort by non-existing property
     */
    public void sort(Object propertyId, SortDirection direction) {
        sort(Sort.by(propertyId, direction));
    }

    /**
     * Clear the current sort order, and re-sort the grid.
     */
    public void clearSortOrder() {
        sortOrder.clear();
        sort(false);
    }

    /**
     * Sets the sort order to use.
     * <p>
     * <em>Note:</em> Sorting by a property that has no column in Grid will hide
     * all possible sorting indicators.
     *
     * @param order
     *            a sort order list.
     *
     * @throws IllegalStateException
     *             if container is not sortable (does not implement
     *             Container.Sortable)
     * @throws IllegalArgumentException
     *             if order is null or trying to sort by non-existing property
     */
    public void setSortOrder(List<SortOrder> order) {
        setSortOrder(order, false);
    }

    private void setSortOrder(List<SortOrder> order, boolean userOriginated)
            throws IllegalStateException, IllegalArgumentException {
        if (!(getContainerDataSource() instanceof Container.Sortable)) {
            throw new IllegalStateException(
                    "Attached container is not sortable (does not implement Container.Sortable)");
        }

        if (order == null) {
            throw new IllegalArgumentException("Order list may not be null!");
        }

        sortOrder.clear();

        Collection<?> sortableProps = ((Container.Sortable) getContainerDataSource())
                .getSortableContainerPropertyIds();

        for (SortOrder o : order) {
            if (!sortableProps.contains(o.getPropertyId())) {
                throw new IllegalArgumentException("Property "
                        + o.getPropertyId()
                        + " does not exist or is not sortable in the current container");
            }
        }

        sortOrder.addAll(order);
        sort(userOriginated);
    }

    /**
     * Get the current sort order list.
     *
     * @return a sort order list
     */
    public List<SortOrder> getSortOrder() {
        return Collections.unmodifiableList(sortOrder);
    }

    /**
     * Apply sorting to data source.
     */
    private void sort(boolean userOriginated) {

        Container c = getContainerDataSource();
        if (c instanceof Container.Sortable) {
            Container.Sortable cs = (Container.Sortable) c;

            final int items = sortOrder.size();
            Object[] propertyIds = new Object[items];
            boolean[] directions = new boolean[items];

            SortDirection[] stateDirs = new SortDirection[items];

            for (int i = 0; i < items; ++i) {
                SortOrder order = sortOrder.get(i);

                stateDirs[i] = order.getDirection();
                propertyIds[i] = order.getPropertyId();
                switch (order.getDirection()) {
                case ASCENDING:
                    directions[i] = true;
                    break;
                case DESCENDING:
                    directions[i] = false;
                    break;
                default:
                    throw new IllegalArgumentException("getDirection() of "
                            + order + " returned an unexpected value");
                }
            }

            cs.sort(propertyIds, directions);

            if (columns.keySet().containsAll(Arrays.asList(propertyIds))) {
                String[] columnKeys = new String[items];
                for (int i = 0; i < items; ++i) {
                    columnKeys[i] = this.columnKeys.key(propertyIds[i]);
                }
                getState().sortColumns = columnKeys;
                getState(false).sortDirs = stateDirs;
            } else {
                // Not all sorted properties are in Grid. Remove any indicators.
                getState().sortColumns = new String[] {};
                getState(false).sortDirs = new SortDirection[] {};
            }
            fireEvent(new SortEvent(this, new ArrayList<SortOrder>(sortOrder),
                    userOriginated));
        } else {
            throw new IllegalStateException(
                    "Container is not sortable (does not implement Container.Sortable)");
        }
    }

    /**
     * Adds a sort order change listener that gets notified when the sort order
     * changes.
     *
     * @param listener
     *            the sort order change listener to add
     */
    @Override
    public void addSortListener(SortListener listener) {
        addListener(SortEvent.class, listener);
    }

    /**
     * Removes a sort order change listener previously added using
     * {@link #addSortListener(SortListener)}.
     *
     * @param listener
     *            the sort order change listener to remove
     */
    @Override
    public void removeSortListener(SortListener listener) {
        removeListener(SortEvent.class, listener);
    }

    /* Grid Headers */

    /**
     * Returns the header section of this grid. The default header contains a
     * single row displaying the column captions.
     *
     * @return the header
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Gets the header row at given index.
     *
     * @param rowIndex
     *            0 based index for row. Counted from top to bottom
     * @return header row at given index
     * @throws IllegalArgumentException
     *             if no row exists at given index
     */
    public HeaderRow getHeaderRow(int rowIndex) {
        return header.getRow(rowIndex);
    }

    /**
     * Inserts a new row at the given position to the header section. Shifts the
     * row currently at that position and any subsequent rows down (adds one to
     * their indices).
     *
     * @param index
     *            the position at which to insert the row
     * @return the new row
     *
     * @throws IllegalArgumentException
     *             if the index is less than 0 or greater than row count
     * @see #appendHeaderRow()
     * @see #prependHeaderRow()
     * @see #removeHeaderRow(HeaderRow)
     * @see #removeHeaderRow(int)
     */
    public HeaderRow addHeaderRowAt(int index) {
        return header.addRowAt(index);
    }

    /**
     * Adds a new row at the bottom of the header section.
     *
     * @return the new row
     * @see #prependHeaderRow()
     * @see #addHeaderRowAt(int)
     * @see #removeHeaderRow(HeaderRow)
     * @see #removeHeaderRow(int)
     */
    public HeaderRow appendHeaderRow() {
        return header.appendRow();
    }

    /**
     * Returns the current default row of the header section. The default row is
     * a special header row providing a user interface for sorting columns.
     * Setting a header text for column updates cells in the default header.
     *
     * @return the default row or null if no default row set
     */
    public HeaderRow getDefaultHeaderRow() {
        return header.getDefaultRow();
    }

    /**
     * Gets the row count for the header section.
     *
     * @return row count
     */
    public int getHeaderRowCount() {
        return header.getRowCount();
    }

    /**
     * Adds a new row at the top of the header section.
     *
     * @return the new row
     * @see #appendHeaderRow()
     * @see #addHeaderRowAt(int)
     * @see #removeHeaderRow(HeaderRow)
     * @see #removeHeaderRow(int)
     */
    public HeaderRow prependHeaderRow() {
        return header.prependRow();
    }

    /**
     * Removes the given row from the header section.
     *
     * @param row
     *            the row to be removed
     *
     * @throws IllegalArgumentException
     *             if the row does not exist in this section
     * @see #removeHeaderRow(int)
     * @see #addHeaderRowAt(int)
     * @see #appendHeaderRow()
     * @see #prependHeaderRow()
     */
    public void removeHeaderRow(HeaderRow row) {
        header.removeRow(row);
    }

    /**
     * Removes the row at the given position from the header section.
     *
     * @param rowIndex
     *            the position of the row
     *
     * @throws IllegalArgumentException
     *             if no row exists at given index
     * @see #removeHeaderRow(HeaderRow)
     * @see #addHeaderRowAt(int)
     * @see #appendHeaderRow()
     * @see #prependHeaderRow()
     */
    public void removeHeaderRow(int rowIndex) {
        header.removeRow(rowIndex);
    }

    /**
     * Sets the default row of the header. The default row is a special header
     * row providing a user interface for sorting columns.
     *
     * @param row
     *            the new default row, or null for no default row
     *
     * @throws IllegalArgumentException
     *             header does not contain the row
     */
    public void setDefaultHeaderRow(HeaderRow row) {
        header.setDefaultRow(row);
    }

    /**
     * Sets the visibility of the header section.
     *
     * @param visible
     *            true to show header section, false to hide
     */
    public void setHeaderVisible(boolean visible) {
        header.setVisible(visible);
    }

    /**
     * Returns the visibility of the header section.
     *
     * @return true if visible, false otherwise.
     */
    public boolean isHeaderVisible() {
        return header.isVisible();
    }

    /* Grid Footers */

    /**
     * Returns the footer section of this grid. The default header contains a
     * single row displaying the column captions.
     *
     * @return the footer
     */
    protected Footer getFooter() {
        return footer;
    }

    /**
     * Gets the footer row at given index.
     *
     * @param rowIndex
     *            0 based index for row. Counted from top to bottom
     * @return footer row at given index
     * @throws IllegalArgumentException
     *             if no row exists at given index
     */
    public FooterRow getFooterRow(int rowIndex) {
        return footer.getRow(rowIndex);
    }

    /**
     * Inserts a new row at the given position to the footer section. Shifts the
     * row currently at that position and any subsequent rows down (adds one to
     * their indices).
     *
     * @param index
     *            the position at which to insert the row
     * @return the new row
     *
     * @throws IllegalArgumentException
     *             if the index is less than 0 or greater than row count
     * @see #appendFooterRow()
     * @see #prependFooterRow()
     * @see #removeFooterRow(FooterRow)
     * @see #removeFooterRow(int)
     */
    public FooterRow addFooterRowAt(int index) {
        return footer.addRowAt(index);
    }

    /**
     * Adds a new row at the bottom of the footer section.
     *
     * @return the new row
     * @see #prependFooterRow()
     * @see #addFooterRowAt(int)
     * @see #removeFooterRow(FooterRow)
     * @see #removeFooterRow(int)
     */
    public FooterRow appendFooterRow() {
        return footer.appendRow();
    }

    /**
     * Gets the row count for the footer.
     *
     * @return row count
     */
    public int getFooterRowCount() {
        return footer.getRowCount();
    }

    /**
     * Adds a new row at the top of the footer section.
     *
     * @return the new row
     * @see #appendFooterRow()
     * @see #addFooterRowAt(int)
     * @see #removeFooterRow(FooterRow)
     * @see #removeFooterRow(int)
     */
    public FooterRow prependFooterRow() {
        return footer.prependRow();
    }

    /**
     * Removes the given row from the footer section.
     *
     * @param row
     *            the row to be removed
     *
     * @throws IllegalArgumentException
     *             if the row does not exist in this section
     * @see #removeFooterRow(int)
     * @see #addFooterRowAt(int)
     * @see #appendFooterRow()
     * @see #prependFooterRow()
     */
    public void removeFooterRow(FooterRow row) {
        footer.removeRow(row);
    }

    /**
     * Removes the row at the given position from the footer section.
     *
     * @param rowIndex
     *            the position of the row
     *
     * @throws IllegalArgumentException
     *             if no row exists at given index
     * @see #removeFooterRow(FooterRow)
     * @see #addFooterRowAt(int)
     * @see #appendFooterRow()
     * @see #prependFooterRow()
     */
    public void removeFooterRow(int rowIndex) {
        footer.removeRow(rowIndex);
    }

    /**
     * Sets the visibility of the footer section.
     *
     * @param visible
     *            true to show footer section, false to hide
     */
    public void setFooterVisible(boolean visible) {
        footer.setVisible(visible);
    }

    /**
     * Returns the visibility of the footer section.
     *
     * @return true if visible, false otherwise.
     */
    public boolean isFooterVisible() {
        return footer.isVisible();
    }

    @Override
    public Iterator<Component> iterator() {
        // This is a hash set to avoid adding header/footer components inside
        // merged cells multiple times
        LinkedHashSet<Component> componentList = new LinkedHashSet<Component>();

        Header header = getHeader();
        for (int i = 0; i < header.getRowCount(); ++i) {
            HeaderRow row = header.getRow(i);
            for (Object propId : columns.keySet()) {
                HeaderCell cell = row.getCell(propId);
                if (cell.getCellState().type == GridStaticCellType.WIDGET) {
                    componentList.add(cell.getComponent());
                }
            }
        }

        Footer footer = getFooter();
        for (int i = 0; i < footer.getRowCount(); ++i) {
            FooterRow row = footer.getRow(i);
            for (Object propId : columns.keySet()) {
                FooterCell cell = row.getCell(propId);
                if (cell.getCellState().type == GridStaticCellType.WIDGET) {
                    componentList.add(cell.getComponent());
                }
            }
        }

        componentList.addAll(getEditorFields());

        return componentList.iterator();
    }

    @Override
    public boolean isRendered(Component childComponent) {
        if (getEditorFields().contains(childComponent)) {
            // Only render editor fields if the editor is open
            return isEditorActive();
        } else {
            // TODO Header and footer components should also only be rendered if
            // the header/footer is visible
            return true;
        }
    }

    EditorClientRpc getEditorRpc() {
        return getRpcProxy(EditorClientRpc.class);
    }

    /**
     * Sets the {@code CellDescriptionGenerator} instance for generating
     * optional descriptions (tooltips) for individual Grid cells. If a
     * {@link RowDescriptionGenerator} is also set, the row description it
     * generates is displayed for cells for which {@code generator} returns
     * null.
     *
     * @param generator
     *            the description generator to use or {@code null} to remove a
     *            previously set generator if any
     *
     * @see #setRowDescriptionGenerator(RowDescriptionGenerator)
     *
     * @since 7.6
     */
    public void setCellDescriptionGenerator(
            CellDescriptionGenerator generator) {
        cellDescriptionGenerator = generator;
        getState().hasDescriptions = (generator != null
                || rowDescriptionGenerator != null);
        dataProvider.refreshCache();
    }

    /**
     * Returns the {@code CellDescriptionGenerator} instance used to generate
     * descriptions (tooltips) for Grid cells.
     *
     * @return the description generator or {@code null} if no generator is set
     *
     * @since 7.6
     */
    public CellDescriptionGenerator getCellDescriptionGenerator() {
        return cellDescriptionGenerator;
    }

    /**
     * Sets the {@code RowDescriptionGenerator} instance for generating optional
     * descriptions (tooltips) for Grid rows. If a
     * {@link CellDescriptionGenerator} is also set, the row description
     * generated by {@code generator} is used for cells for which the cell
     * description generator returns null.
     *
     *
     * @param generator
     *            the description generator to use or {@code null} to remove a
     *            previously set generator if any
     *
     * @see #setCellDescriptionGenerator(CellDescriptionGenerator)
     *
     * @since 7.6
     */
    public void setRowDescriptionGenerator(RowDescriptionGenerator generator) {
        rowDescriptionGenerator = generator;
        getState().hasDescriptions = (generator != null
                || cellDescriptionGenerator != null);
        dataProvider.refreshCache();
    }

    /**
     * Returns the {@code RowDescriptionGenerator} instance used to generate
     * descriptions (tooltips) for Grid rows
     *
     * @return the description generator or {@code} null if no generator is set
     *
     * @since 7.6
     */
    public RowDescriptionGenerator getRowDescriptionGenerator() {
        return rowDescriptionGenerator;
    }

    /**
     * Sets the style generator that is used for generating styles for cells
     *
     * @param cellStyleGenerator
     *            the cell style generator to set, or <code>null</code> to
     *            remove a previously set generator
     */
    public void setCellStyleGenerator(CellStyleGenerator cellStyleGenerator) {
        this.cellStyleGenerator = cellStyleGenerator;
        dataProvider.refreshCache();
    }

    /**
     * Gets the style generator that is used for generating styles for cells
     *
     * @return the cell style generator, or <code>null</code> if no generator is
     *         set
     */
    public CellStyleGenerator getCellStyleGenerator() {
        return cellStyleGenerator;
    }

    /**
     * Sets the style generator that is used for generating styles for rows
     *
     * @param rowStyleGenerator
     *            the row style generator to set, or <code>null</code> to remove
     *            a previously set generator
     */
    public void setRowStyleGenerator(RowStyleGenerator rowStyleGenerator) {
        this.rowStyleGenerator = rowStyleGenerator;
        dataProvider.refreshCache();
    }

    /**
     * Gets the style generator that is used for generating styles for rows
     *
     * @return the row style generator, or <code>null</code> if no generator is
     *         set
     */
    public RowStyleGenerator getRowStyleGenerator() {
        return rowStyleGenerator;
    }

    /**
     * Adds a row to the underlying container. The order of the parameters
     * should match the current visible column order.
     * <p>
     * Please note that it's generally only safe to use this method during
     * initialization. After Grid has been initialized and the visible column
     * order might have been changed, it's better to instead add items directly
     * to the underlying container and use {@link Item#getItemProperty(Object)}
     * to make sure each value is assigned to the intended property.
     *
     * @param values
     *            the cell values of the new row, in the same order as the
     *            visible column order, not <code>null</code>.
     * @return the item id of the new row
     * @throws IllegalArgumentException
     *             if values is null
     * @throws IllegalArgumentException
     *             if its length does not match the number of visible columns
     * @throws IllegalArgumentException
     *             if a parameter value is not an instance of the corresponding
     *             property type
     * @throws UnsupportedOperationException
     *             if the container does not support adding new items
     */
    public Object addRow(Object... values) {
        if (values == null) {
            throw new IllegalArgumentException("Values cannot be null");
        }

        Indexed dataSource = getContainerDataSource();

        List<Column> columns = getColumns();
        if (values.length != columns.size()) {
            throw new IllegalArgumentException(
                    "There are " + columns.size() + " visible columns, but "
                            + values.length + " cell values were provided.");
        }

        // First verify all parameter types
        for (int i = 0; i < columns.size(); i++) {
            Column c = columns.get(i);
            Object propertyId = c.getPropertyId();

            Class<?> propertyType = dataSource.getType(propertyId);
            if (values[i] != null && !propertyType.isInstance(values[i])) {
                throw new IllegalArgumentException("Parameter " + i + "("
                        + values[i] + ") is not an instance of "
                        + propertyType.getCanonicalName());
            }
        }

        Object itemId = dataSource.addItem();
        try {
            Item item = dataSource.getItem(itemId);
            for (int i = 0; i < columns.size(); i++) {
                Object propertyId = columns.get(i).getPropertyId();
                Property<Object> property = item.getItemProperty(propertyId);
                property.setValue(values[i]);
            }
        } catch (RuntimeException e) {
            try {
                dataSource.removeItem(itemId);
            } catch (Exception e2) {
                getLogger().log(Level.SEVERE,
                        "Error recovering from exception in addRow", e);
            }
            throw e;
        }

        return itemId;
    }

    private static Logger getLogger() {
        return Logger.getLogger(Grid.class.getName());
    }

    /**
     * Sets whether or not the item editor UI is enabled for this grid. When the
     * editor is enabled, the user can open it by double-clicking a row or
     * hitting enter when a row is focused. The editor can also be opened
     * programmatically using the {@link #editItem(Object)} method.
     *
     * @param isEnabled
     *            <code>true</code> to enable the feature, <code>false</code>
     *            otherwise
     * @throws IllegalStateException
     *             if an item is currently being edited
     *
     * @see #getEditedItemId()
     */
    public void setEditorEnabled(boolean isEnabled)
            throws IllegalStateException {
        if (isEditorActive()) {
            throw new IllegalStateException(
                    "Cannot disable the editor while an item ("
                            + getEditedItemId() + ") is being edited");
        }
        if (isEditorEnabled() != isEnabled) {
            getState().editorEnabled = isEnabled;
        }
    }

    /**
     * Checks whether the item editor UI is enabled for this grid.
     *
     * @return <code>true</code> iff the editor is enabled for this grid
     *
     * @see #setEditorEnabled(boolean)
     * @see #getEditedItemId()
     */
    public boolean isEditorEnabled() {
        return getState(false).editorEnabled;
    }

    /**
     * Gets the id of the item that is currently being edited.
     *
     * @return the id of the item that is currently being edited, or
     *         <code>null</code> if no item is being edited at the moment
     */
    public Object getEditedItemId() {
        return editedItemId;
    }

    /**
     * Gets the field group that is backing the item editor of this grid.
     *
     * @return the backing field group
     */
    public FieldGroup getEditorFieldGroup() {
        return editorFieldGroup;
    }

    /**
     * Sets the field group that is backing the item editor of this grid.
     *
     * @param fieldGroup
     *            the backing field group
     *
     * @throws IllegalStateException
     *             if the editor is currently active
     */
    public void setEditorFieldGroup(FieldGroup fieldGroup) {
        if (isEditorActive()) {
            throw new IllegalStateException(
                    "Cannot change field group while an item ("
                            + getEditedItemId() + ") is being edited");
        }
        editorFieldGroup = fieldGroup;
    }

    /**
     * Returns whether an item is currently being edited in the editor.
     *
     * @return true iff the editor is open
     */
    public boolean isEditorActive() {
        return editorActive;
    }

    private void checkColumnExists(Object propertyId) {
        if (getColumn(propertyId) == null) {
            throw new IllegalArgumentException(
                    "There is no column with the property id " + propertyId);
        }
    }

    Field<?> getEditorField(Object propertyId) {
        checkColumnExists(propertyId);

        if (!getColumn(propertyId).isEditable()) {
            return null;
        }

        Field<?> editor = editorFieldGroup.getField(propertyId);

        try {
            if (editor == null) {
                editor = editorFieldGroup.buildAndBind(propertyId);
            }
        } finally {
            if (editor == null) {
                editor = editorFieldGroup.getField(propertyId);
            }

            if (editor != null && editor.getParent() != Grid.this) {
                assert editor.getParent() == null;
                editor.setParent(this);
            }
        }
        return editor;
    }

    /**
     * Opens the editor interface for the provided item. Scrolls the Grid to
     * bring the item to view if it is not already visible.
     *
     * Note that any cell content rendered by a WidgetRenderer will not be
     * visible in the editor row.
     *
     * @param itemId
     *            the id of the item to edit
     * @throws IllegalStateException
     *             if the editor is not enabled or already editing an item in
     *             buffered mode
     * @throws IllegalArgumentException
     *             if the {@code itemId} is not in the backing container
     * @see #setEditorEnabled(boolean)
     */
    public void editItem(Object itemId)
            throws IllegalStateException, IllegalArgumentException {
        if (!isEditorEnabled()) {
            throw new IllegalStateException("Item editor is not enabled");
        } else if (isEditorBuffered() && editedItemId != null) {
            throw new IllegalStateException("Editing item " + itemId
                    + " failed. Item editor is already editing item "
                    + editedItemId);
        } else if (!getContainerDataSource().containsId(itemId)) {
            throw new IllegalArgumentException("Item with id " + itemId
                    + " not found in current container");
        }
        editedItemId = itemId;
        getEditorRpc().bind(getContainerDataSource().indexOfId(itemId));
    }

    protected void doEditItem() {
        Item item = getContainerDataSource().getItem(editedItemId);

        editorFieldGroup.setItemDataSource(item);

        for (Column column : getColumns()) {
            // FIXME
            // column.getState().editorConnector = getEditorField(
            // column.getPropertyId());
        }

        editorActive = true;
        // Must ensure that all fields, recursively, are sent to the client
        // This is needed because the fields are hidden using isRendered
        for (Field<?> f : getEditorFields()) {
            f.markAsDirtyRecursive();
        }

        if (datasource instanceof ItemSetChangeNotifier) {
            ((ItemSetChangeNotifier) datasource)
                    .addItemSetChangeListener(editorClosingItemSetListener);
        }
    }

    void setEditorField(Object propertyId, Field<?> field) {
        checkColumnExists(propertyId);

        Field<?> oldField = editorFieldGroup.getField(propertyId);
        if (oldField != null) {
            editorFieldGroup.unbind(oldField);
            oldField.setParent(null);
        }

        if (field != null) {
            field.setParent(this);
            editorFieldGroup.bind(field, propertyId);
        }
    }

    /**
     * Saves all changes done to the bound fields.
     * <p>
     * <em>Note:</em> This is a pass-through call to the backing field group.
     *
     * @throws CommitException
     *             If the commit was aborted
     *
     * @see FieldGroup#commit()
     */
    public void saveEditor() throws CommitException {
        editorFieldGroup.commit();
    }

    /**
     * Cancels the currently active edit if any. Hides the editor and discards
     * possible unsaved changes in the editor fields.
     */
    public void cancelEditor() {
        if (isEditorActive()) {
            getEditorRpc()
                    .cancel(getContainerDataSource().indexOfId(editedItemId));
            doCancelEditor();
        }
    }

    protected void doCancelEditor() {
        editedItemId = null;
        editorActive = false;
        editorFieldGroup.discard();
        editorFieldGroup.setItemDataSource(null);

        if (datasource instanceof ItemSetChangeNotifier) {
            ((ItemSetChangeNotifier) datasource)
                    .removeItemSetChangeListener(editorClosingItemSetListener);
        }

        // Mark Grid as dirty so the client side gets to know that the editors
        // are no longer attached
        markAsDirty();
    }

    void resetEditor() {
        if (isEditorActive()) {
            /*
             * Simply force cancel the editing; throwing here would just make
             * Grid.setContainerDataSource semantics more complicated.
             */
            cancelEditor();
        }
        for (Field<?> editor : getEditorFields()) {
            editor.setParent(null);
        }

        editedItemId = null;
        editorActive = false;
        editorFieldGroup = new CustomFieldGroup(this);
    }

    /**
     * Gets a collection of all fields bound to the item editor of this grid.
     * <p>
     * When {@link #editItem(Object) editItem} is called, fields are
     * automatically created and bound to any unbound properties.
     *
     * @return a collection of all the fields bound to the item editor
     */
    Collection<Field<?>> getEditorFields() {
        Collection<Field<?>> fields = editorFieldGroup.getFields();
        assert allAttached(fields);
        return fields;
    }

    private boolean allAttached(Collection<? extends Component> components) {
        for (Component component : components) {
            if (component.getParent() != this) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the field factory for the {@link FieldGroup}. The field factory is
     * only used when {@link FieldGroup} creates a new field.
     * <p>
     * <em>Note:</em> This is a pass-through call to the backing field group.
     *
     * @param fieldFactory
     *            The field factory to use
     */
    public void setEditorFieldFactory(FieldGroupFieldFactory fieldFactory) {
        editorFieldGroup.setFieldFactory(fieldFactory);
    }

    /**
     * Sets the error handler for the editor.
     *
     * The error handler is called whenever there is an exception in the editor.
     *
     * @param editorErrorHandler
     *            The editor error handler to use
     * @throws IllegalArgumentException
     *             if the error handler is null
     */
    public void setEditorErrorHandler(EditorErrorHandler editorErrorHandler)
            throws IllegalArgumentException {
        if (editorErrorHandler == null) {
            throw new IllegalArgumentException(
                    "The error handler cannot be null");
        }
        this.editorErrorHandler = editorErrorHandler;
    }

    /**
     * Gets the error handler used for the editor
     *
     * @see #setErrorHandler(com.vaadin.server.ErrorHandler)
     * @return the editor error handler, never null
     */
    public EditorErrorHandler getEditorErrorHandler() {
        return editorErrorHandler;
    }

    /**
     * Gets the field factory for the {@link FieldGroup}. The field factory is
     * only used when {@link FieldGroup} creates a new field.
     * <p>
     * <em>Note:</em> This is a pass-through call to the backing field group.
     *
     * @return The field factory in use
     */
    public FieldGroupFieldFactory getEditorFieldFactory() {
        return editorFieldGroup.getFieldFactory();
    }

    /**
     * Sets the caption on the save button in the Grid editor.
     *
     * @param saveCaption
     *            the caption to set
     * @throws IllegalArgumentException
     *             if {@code saveCaption} is {@code null}
     */
    public void setEditorSaveCaption(String saveCaption)
            throws IllegalArgumentException {
        if (saveCaption == null) {
            throw new IllegalArgumentException("Save caption cannot be null");
        }
        getState().editorSaveCaption = saveCaption;
    }

    /**
     * Gets the current caption of the save button in the Grid editor.
     *
     * @return the current caption of the save button
     */
    public String getEditorSaveCaption() {
        return getState(false).editorSaveCaption;
    }

    /**
     * Sets the caption on the cancel button in the Grid editor.
     *
     * @param cancelCaption
     *            the caption to set
     * @throws IllegalArgumentException
     *             if {@code cancelCaption} is {@code null}
     */
    public void setEditorCancelCaption(String cancelCaption)
            throws IllegalArgumentException {
        if (cancelCaption == null) {
            throw new IllegalArgumentException("Cancel caption cannot be null");
        }
        getState().editorCancelCaption = cancelCaption;
    }

    /**
     * Gets the current caption of the cancel button in the Grid editor.
     *
     * @return the current caption of the cancel button
     */
    public String getEditorCancelCaption() {
        return getState(false).editorCancelCaption;
    }

    // /**
    // * Add an editor event listener
    // *
    // * @param listener
    // * the event listener object to add
    // */
    // public void addEditorListener(EditorListener listener) {
    // addListener(EditorOpenEvent.class, listener);
    // addListener(GridConstants.EDITOR_MOVE_EVENT_ID, EditorMoveEvent.class,
    // listener, EditorListener.EDITOR_MOVE_METHOD);
    // addListener(GridConstants.EDITOR_CLOSE_EVENT_ID, EditorCloseEvent.class,
    // listener, EditorListener.EDITOR_CLOSE_METHOD);
    // }
    //
    // /**
    // * Remove an editor event listener
    // *
    // * @param listener
    // * the event listener object to remove
    // */
    // public void removeEditorListener(EditorListener listener) {
    // removeListener(GridConstants.EDITOR_OPEN_EVENT_ID,
    // EditorOpenEvent.class, listener);
    // removeListener(GridConstants.EDITOR_MOVE_EVENT_ID,
    // EditorMoveEvent.class, listener);
    // removeListener(GridConstants.EDITOR_CLOSE_EVENT_ID,
    // EditorCloseEvent.class, listener);
    // }

    /**
     * Sets the buffered editor mode. The default mode is buffered (
     * <code>true</code>).
     *
     * @since 7.6
     * @param editorBuffered
     *            <code>true</code> to enable buffered editor,
     *            <code>false</code> to disable it
     * @throws IllegalStateException
     *             If editor is active while attempting to change the buffered
     *             mode.
     */
    public void setEditorBuffered(boolean editorBuffered)
            throws IllegalStateException {
        if (isEditorActive()) {
            throw new IllegalStateException(
                    "Can't change editor unbuffered mode while editor is active.");
        }
        getState().editorBuffered = editorBuffered;
        editorFieldGroup.setBuffered(editorBuffered);
    }

    /**
     * Gets the buffered editor mode.
     *
     * @since 7.6
     * @return <code>true</code> if buffered editor is enabled,
     *         <code>false</code> otherwise
     */
    public boolean isEditorBuffered() {
        return getState(false).editorBuffered;
    }

    @Override
    public void addItemClickListener(ItemClickListener listener) {
        addListener(ItemClickEvent.class, listener);
    }

    @Override
    public void removeItemClickListener(ItemClickListener listener) {
        removeListener(ItemClickEvent.class, listener);
    }

    /**
     * Requests that the column widths should be recalculated.
     * <p>
     * In most cases Grid will know when column widths need to be recalculated
     * but this method can be used to force recalculation in situations when
     * grid does not recalculate automatically.
     *
     * @since 7.4.1
     */
    public void recalculateColumnWidths() {
        getRpcProxy(GridClientRpc.class).recalculateColumnWidths();
    }

    /**
     * Registers a new column visibility change listener
     *
     * @since 7.5.0
     * @param listener
     *            the listener to register
     */
    public void addColumnVisibilityChangeListener(
            ColumnVisibilityChangeListener listener) {
        addListener(ColumnVisibilityChangeEvent.class, listener);
    }

    /**
     * Removes a previously registered column visibility change listener
     *
     * @since 7.5.0
     * @param listener
     *            the listener to remove
     */
    public void removeColumnVisibilityChangeListener(
            ColumnVisibilityChangeListener listener) {
        removeListener(ColumnVisibilityChangeEvent.class, listener);
    }

    void fireColumnVisibilityChangeEvent(Column column, boolean hidden,
            boolean isUserOriginated) {
        fireEvent(new ColumnVisibilityChangeEvent(this, column, hidden,
                isUserOriginated));
    }

    private static SelectionMode getDefaultSelectionMode() {
        return SelectionMode.SINGLE;
    }

}
