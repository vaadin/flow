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

package com.vaadin.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.annotations.JavaScriptModule;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Container.Indexed.ItemAddEvent;
import com.vaadin.data.Container.Indexed.ItemRemoveEvent;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Container.ItemSetChangeNotifier;
import com.vaadin.data.DataGenerator;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Property.ValueChangeNotifier;
import com.vaadin.data.sort.SortOrder;
import com.vaadin.hummingbird.kernel.DomEventListener;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.KeyMapper;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.grid.GridState;
import com.vaadin.ui.Grid.Column;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Provides Vaadin server-side container data source to a
 * {@link com.vaadin.client.ui.grid.GridConnector}.
 *
 * @since 7.4
 * @author Vaadin Ltd
 */
public class GridJSDataProvider {

    private Indexed container;
    private final ActiveItemHandler activeItemHandler = new ActiveItemHandler();

    /**
     * Class for keeping track of current items and ValueChangeListeners.
     *
     * @since 7.6
     */
    private class ActiveItemHandler implements Serializable, DataGenerator {
        private final Map<Object, GridValueChangeListener> activeItemMap = new HashMap<Object, GridValueChangeListener>();
        private final KeyMapper<Object> keyMapper = new KeyMapper<Object>();
        private final Set<Object> droppedItems = new HashSet<Object>();

        /**
         * Registers ValueChangeListeners for given item ids.
         * <p>
         * Note: This method will clean up any unneeded listeners and key
         * mappings
         *
         * @param itemIds
         *            collection of new active item ids
         */
        public void addActiveItems(Collection<?> itemIds) {
            for (Object itemId : itemIds) {
                if (!activeItemMap.containsKey(itemId)) {
                    activeItemMap.put(itemId, new GridValueChangeListener(
                            itemId, container.getItem(itemId)));
                }
            }

            // Remove still active rows that were "dropped"
            droppedItems.removeAll(itemIds);
            internalDropActiveItems(droppedItems);
            droppedItems.clear();
        }

        /**
         * Marks given item id as dropped. Dropped items are cleared when adding
         * new active items.
         *
         * @param itemId
         *            dropped item id
         */
        public void dropActiveItem(Object itemId) {
            if (activeItemMap.containsKey(itemId)) {
                droppedItems.add(itemId);
            }
        }

        private void internalDropActiveItems(Collection<Object> itemIds) {
            for (Object itemId : droppedItems) {
                assert activeItemMap.containsKey(
                        itemId) : "Item ID should exist in the activeItemMap";

                activeItemMap.remove(itemId).removeListener();
                keyMapper.remove(itemId);
            }
        }

        /**
         * Gets a collection copy of currently active item ids.
         *
         * @return collection of item ids
         */
        public Collection<Object> getActiveItemIds() {
            return new HashSet<Object>(activeItemMap.keySet());
        }

        /**
         * Gets a collection copy of currently active ValueChangeListeners.
         *
         * @return collection of value change listeners
         */
        public Collection<GridValueChangeListener> getValueChangeListeners() {
            return new HashSet<GridValueChangeListener>(activeItemMap.values());
        }

        @Override
        public void generateData(Object itemId, Item item, JsonObject rowData) {
            rowData.put(GridState.JSONKEY_ROWKEY, keyMapper.key(itemId));
        }

        public void clear() {
            for (GridValueChangeListener l : getValueChangeListeners()) {
                l.removeListener();
            }

            activeItemMap.clear();
            keyMapper.removeAll();
            droppedItems.clear();

        }
    }

    /**
     * A class to listen to changes in property values in the Container added
     * with {@link Grid#setContainerDatasource(Container.Indexed)}, and notifies
     * the data source to update the client-side representation of the modified
     * item.
     * <p>
     * One instance of this class can (and should) be reused for all the
     * properties in an item, since this class will inform that the entire row
     * needs to be re-evaluated (in contrast to a property-based change
     * management)
     * <p>
     * Since there's no Container-wide possibility to listen to any kind of
     * value changes, an instance of this class needs to be attached to each and
     * every Item's Property in the container.
     *
     * @see Grid#addValueChangeListener(Container, Object, Object)
     * @see Grid#valueChangeListeners
     */
    private class GridValueChangeListener implements ValueChangeListener {
        private final Object itemId;
        private final Item item;

        public GridValueChangeListener(Object itemId, Item item) {
            /*
             * Using an assert instead of an exception throw, just to optimize
             * prematurely
             */
            assert itemId != null : "null itemId not accepted";
            this.itemId = itemId;
            this.item = item;
            internalAddColumns(grid.getColumns());
        }

        @Override
        public void valueChange(ValueChangeEvent event) {
            updateRowData(itemId);
        }

        public void removeListener() {
            removeColumns(grid.getColumns());
        }

        public void addColumns(Collection<Column> addedColumns) {
            internalAddColumns(addedColumns);
            updateRowData(itemId);
        }

        private void internalAddColumns(Collection<Column> addedColumns) {
            for (final Column column : addedColumns) {
                final Property<?> property = item
                        .getItemProperty(column.getPropertyId());
                if (property instanceof ValueChangeNotifier) {
                    ((ValueChangeNotifier) property)
                            .addValueChangeListener(this);
                }
            }
        }

        public void removeColumns(Collection<Column> removedColumns) {
            for (final Column column : removedColumns) {
                final Property<?> property = item
                        .getItemProperty(column.getPropertyId());
                if (property instanceof ValueChangeNotifier) {
                    ((ValueChangeNotifier) property)
                            .removeValueChangeListener(this);
                }
            }
        }
    }

    private final ItemSetChangeListener itemListener = new ItemSetChangeListener() {
        @Override
        public void containerItemSetChange(ItemSetChangeEvent event) {

            if (event instanceof ItemAddEvent) {
                ItemAddEvent addEvent = (ItemAddEvent) event;
                int firstIndex = addEvent.getFirstIndex();
                int count = addEvent.getAddedItemsCount();
                insertRowData(firstIndex, count);
            }

            else if (event instanceof ItemRemoveEvent) {
                ItemRemoveEvent removeEvent = (ItemRemoveEvent) event;
                int firstIndex = removeEvent.getFirstIndex();
                int count = removeEvent.getRemovedItemsCount();
                removeRowData(firstIndex, count);
            }

            else {

                /*
                 * Clear everything we have in view, and let the client
                 * re-request for whatever it needs.
                 *
                 * Why this shortcut? Well, since anything could've happened, we
                 * don't know what has happened. There are a lot of use-cases we
                 * can cover at once with this carte blanche operation:
                 *
                 * 1) Grid is scrolled somewhere in the middle and all the
                 * rows-inview are removed. We need a new pageful.
                 *
                 * 2) Grid is scrolled somewhere in the middle and none of the
                 * visible rows are removed. We need no new rows.
                 *
                 * 3) Grid is scrolled all the way to the bottom, and the last
                 * rows are being removed. Grid needs to scroll up and request
                 * for more rows at the top.
                 *
                 * 4) Grid is scrolled pretty much to the bottom, and the last
                 * rows are being removed. Grid needs to be aware that some
                 * scrolling is needed, but not to compensate for all the
                 * removed rows. And it also needs to request for some more rows
                 * to the top.
                 *
                 * 5) Some ranges of rows are removed from view. We need to
                 * collapse the gaps with existing rows and load the missing
                 * rows.
                 *
                 * 6) The ultimate use case! Grid has 1.5 pages of rows and
                 * scrolled a bit down. One page of rows is removed. We need to
                 * make sure that new rows are loaded, but not all old slots are
                 * occupied, since the page can't be filled with new row data.
                 * It also needs to be scrolled to the top.
                 *
                 * So, it's easier (and safer) to do the simple thing instead of
                 * taking all the corner cases into account.
                 */

                sendAllData = true;
                sendChangesLater();
            }
        }
    };

    private List<RowChange> rowChanges = new ArrayList<RowChange>();

    private static abstract class RowChange {
        private int index;

        public RowChange(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

    }

    private static class InsertChange extends RowChange {

        private List<?> itemIds;

        public InsertChange(int index, List<?> itemIds) {
            super(index);
            this.itemIds = itemIds;
        }

        public List<?> getItemIds() {
            return itemIds;
        }
    }

    private static class RemoveChange extends RowChange {
        private int count;

        public RemoveChange(int index, int count) {
            super(index);
            this.count = count;
        }

        public int getCount() {
            return count;
        }

    }

    private static class UpdateChange extends RowChange {

        private Object itemId;

        public UpdateChange(int index, Object itemId) {
            super(index);
            this.itemId = itemId;
        }

        public Object getItemId() {
            return itemId;
        }

    }

    /** Size possibly changed with a bare ItemSetChangeEvent */
    private boolean sendAllData = false;

    private final Set<DataGenerator> dataGenerators = new LinkedHashSet<DataGenerator>();

    private Grid grid;
    private Element element;

    private Runnable sendDataSource = this::sendDataSource;

    private boolean initial = true;

    /**
     * Creates a new data provider using the given container.
     *
     * @param grid
     *            the container to make available
     */
    public GridJSDataProvider(Grid grid) {
        attach(grid);

    }

    private void pushRowData(int firstRowToPush, int numberOfRows,
            int firstCachedRowIndex, int cacheSize, String callbackId) {

        List<?> itemIds = container.getItemIds(firstRowToPush, numberOfRows);
        JsonArray rows = serializeItems(itemIds);

        activeItemHandler.addActiveItems(itemIds);
        getJS().provideRows(element, firstRowToPush, rows, container.size(),
                callbackId);
    }

    /**
     * Makes the data source available to the given {@link Grid} component.
     *
     * @param component
     *            the remote data grid component to extend
     * @param columnKeys
     *            the key mapper for columns
     */
    private void attach(Grid component) {
        resetState();
        grid = component;
        element = grid.getElement();
        initial = true;

        Element element = this.element;

        // Listen to requests for more data
        element.addEventData("hData", "id", "index", "count", "cacheFirst",
                "cacheCount", "element.sortOrder");
        element.addEventListener("hData", elementDataRequestListener);

        getJS().init(element);
        sendChangesLater();
    }

    private DomEventListener elementDataRequestListener = e -> {
        String id = e.getString("id");
        int index = (int) e.getNumber("index");
        int count = (int) e.getNumber("count");
        int cacheFirst = (int) e.getNumber("cacheFirst");
        int cacheCount = (int) e.getNumber("cacheCount");
        JsonArray clientSortOrder = e.getArray("element.sortOrder");

        if (clientSortOrder == null) {
            clientSortOrder = Json.createArray();
        }

        ArrayList<SortOrder> newSortOrder = new ArrayList<>();
        for (int i = 0; i < clientSortOrder.length(); i++) {
            JsonObject columnOrder = clientSortOrder.getObject(i);
            int columnIndex = (int) columnOrder.getNumber("column");
            String direction = columnOrder.getString("direction");

            SortDirection directionEnum = Grid.sortDirections.get(direction);
            if (directionEnum == null) {
                throw new RuntimeException(
                        "Unsupported sort direction: " + direction);
            }

            newSortOrder.add(new SortOrder(
                    grid.getColumns().get(columnIndex).getPropertyId(),
                    directionEnum));
        }

        if (!grid.getSortOrder().equals(newSortOrder)) {
            grid.setSortOrder(newSortOrder);
        }

        pushRowData(index, count, cacheFirst, cacheCount, id);
    };

    private void resetState() {
        rowChanges.clear();
        sendAllData = false;
    }

    private void sendChangesLater() {
        element.runBeforeNextClientResponse(sendDataSource);
    }

    private void sendDataSource() {
        if (initial || sendAllData) {
            /*
             * Push initial set of rows, assuming Grid will initially be
             * rendered scrolled to the top and with a decent amount of rows
             * visible. If this guess is right, initial data can be shown
             * without a round-trip and if it's wrong, the data will simply be
             * discarded.
             */
            int datasourceSize = container.size();
            getJS().invalidateCache(element, datasourceSize);
            pushRowData(0, Math.min(50, datasourceSize), 0, 0, null);
        } else {
            Set<Object> insertedIds = new HashSet<>();
            // Send updates
            for (RowChange r : rowChanges) {
                if (r instanceof InsertChange) {
                    InsertChange ic = (InsertChange) r;
                    JsonArray data = Json.createArray();

                    for (Object itemId : ic.getItemIds()) {
                        JsonArray row = null;
                        if (container.containsId(itemId)) {
                            row = serializeItem(itemId);
                        }
                        data.set(data.length(), row);
                    }
                    getJS().insertRows(element, ic.getIndex(), data);
                    insertedIds.addAll(ic.getItemIds());
                } else if (r instanceof RemoveChange) {
                    RemoveChange rc = (RemoveChange) r;
                    getJS().removeRows(element, rc.getIndex(), rc.getCount());
                } else if (r instanceof UpdateChange) {
                    UpdateChange uc = (UpdateChange) r;
                    JsonArray row = serializeItem(uc.getItemId());
                    getJS().updateRow(element, uc.getIndex(), row);
                }
            }
            activeItemHandler.addActiveItems(insertedIds);

        }

        // Clear all changes.
        resetState();
        initial = false;
    }

    private JsonArray serializeItems(List<?> itemIds) {
        JsonArray rows = Json.createArray();
        for (Object itemId : itemIds) {
            JsonArray row = serializeItem(itemId);
            rows.set(rows.length(), row);
        }
        return rows;
    }

    private JsonArray serializeItem(Object itemId) {
        Item item = container.getItem(itemId);

        JsonArray row = Json.createArray();
        for (Column column : grid.getColumns()) {
            Object value = item.getItemProperty(column.getPropertyId())
                    .getValue();
            row.set(row.length(), String.valueOf(value));
        }
        return row;
    }

    @JavaScriptModule("Grid.js")
    public interface JS {
        public void init(Element context);

        public void updateRow(Element element, int index, JsonArray rows);

        public void provideRows(Element context, int firstIndex, JsonArray rows,
                int totalSize, String id);

        public void provideRows(Element context, int firstIndex, JsonArray rows,
                int totalSize);

        public void invalidateCache(Element context, int containerSize);

        public void insertRows(Element context, int index, JsonArray rows);

        public void removeRows(Element context, int index, int count);
    }

    private JS getJS() {
        return com.vaadin.ui.JS.get(JS.class, element);
    }

    /**
     * Informs the client side that new rows have been inserted into the data
     * source.
     *
     * @param index
     *            the index at which new rows have been inserted
     * @param count
     *            the number of rows inserted at <code>index</code>
     */
    private void insertRowData(final int index, final int count) {
        if (sendAllData) {
            // Don't bother, the changes will be ignored anyway
            return;
        }
        /*
         * Since all changes should be processed in a consistent order, we don't
         * send the RPC call immediately. beforeClientResponse will decide
         * whether to send these or not. Valid situation to not send these is
         * initial response or bare ItemSetChange event.
         */

        List<?> itemIds = new ArrayList(container.getItemIds(index, count));
        rowChanges.add(new InsertChange(index, itemIds));
        sendChangesLater();
    }

    /**
     * Informs the client side that rows have been removed from the data source.
     *
     * @param index
     *            the index of the first row removed
     * @param count
     *            the number of rows removed
     * @param firstItemId
     *            the item id of the first removed item
     */
    private void removeRowData(final int index, final int count) {
        if (sendAllData) {
            // Don't bother, the changes will be ignored anyway
            return;
        }

        rowChanges.add(new RemoveChange(index, count));
        sendChangesLater();
    }

    /**
     * Informs the client side that data of a row has been modified in the data
     * source.
     *
     * @param itemId
     *            the item Id the row that was updated
     */
    public void updateRowData(Object itemId) {
        rowChanges.add(new UpdateChange(container.indexOfId(itemId), itemId));
        sendChangesLater();
    }

    /**
     * Pushes a new version of all the rows in the active cache range.
     */
    public void refreshCache() {
        if (!sendAllData) {
            sendAllData = true;
            sendChangesLater();
        }
    }

    public void setContainer(Indexed container) {
        if (this.container != null) {
            // We're being detached, release various listeners
            if (container instanceof ItemSetChangeNotifier) {
                ((ItemSetChangeNotifier) container)
                        .removeItemSetChangeListener(itemListener);
            }

            activeItemHandler.clear();
        }

        this.container = container;
        if (container instanceof ItemSetChangeNotifier) {
            ((ItemSetChangeNotifier) container)
                    .addItemSetChangeListener(itemListener);
        }

        resetState();
        refreshCache();
    }

}
