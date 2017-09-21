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
package com.vaadin.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.swing.SingleSelectionModel;

import com.vaadin.data.Binder;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.util.HtmlUtils;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.shared.Registration;
import com.vaadin.ui.ArrayUpdater.Update;
import com.vaadin.ui.common.AttachEvent;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.event.Tag;

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
public class Grid<T> extends Component implements HasDataProvider<T> {

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
     * to easily switch between the build-in selection models.
     *
     * @see Grid#setSelectionMode(SelectionMode)
     * @see Grid#setSelectionModel(GridSelectionModel)
     */
    public enum SelectionMode {

        /**
         * Single selection mode that maps to built-in
         * {@link SingleSelectionModel}.
         *
         * @see SingleSelectionModelImpl
         */
        SINGLE {
            @Override
            protected <T> GridSelectionModel<T> createModel() {
                return new GridSingleSelectionModel<T>() {

                    @Override
                    public Set<T> getSelectedItems() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public Optional<T> getFirstSelectedItem() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public void select(T item) {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public void deselect(T item) {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public void deselectAll() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public Registration addSelectionListener(
                            SelectionListener<T> listener) {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public Optional<T> getSelectedItem() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public void setDeselectAllowed(boolean deselectAllowed) {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public boolean isDeselectAllowed() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }

                    @Override
                    public SingleSelect<? extends Grid<T>, T> asSingleSelect() {
                        throw new UnsupportedOperationException(
                                "Not implemented yet.");
                    }
                };
            }
        },

        /**
         * Multiselection mode that maps to built-in {@link MultiSelectionModel}
         * .
         *
         * @see MultiSelectionModelImpl
         */
        MULTI {
            @Override
            protected <T> GridSelectionModel<T> createModel() {
                throw new UnsupportedOperationException("Not implemented yet.");
            }
        },

        /**
         * Selection model that doesn't allow selection.
         *
         * @see NoSelectionModel
         */
        NONE {
            @Override
            protected <T> GridSelectionModel<T> createModel() {
                return new GridSelectionModel<T>() {

                    @Override
                    public Set<T> getSelectedItems() {
                        return Collections.emptySet();
                    }

                    @Override
                    public Optional<T> getFirstSelectedItem() {
                        return Optional.empty();
                    }

                    @Override
                    public void select(T item) {
                    }

                    @Override
                    public void deselect(T item) {
                    }

                    @Override
                    public void deselectAll() {
                    }

                    @Override
                    public Registration addSelectionListener(
                            SelectionListener<T> listener) {
                        throw new UnsupportedOperationException(
                                "This selection model doesn't allow selection, cannot add selection listeners to it");
                    }

                    @Override
                    public void remove() {
                    }
                };
            }
        };

        /**
         * Creates the selection model to use with this enum.
         *
         * @param <T>
         *            the type of items in the grid
         * @return the selection model
         */
        protected abstract <T> GridSelectionModel<T> createModel();
    }

    private int pageSize = 100;

    private final ArrayUpdater arrayUpdater = UpdateQueue::new;

    private final Map<String, Function<T, JsonValue>> columnGenerators = new HashMap<>();
    private final DataCommunicator<T> dataCommunicator = new DataCommunicator<>(
            this::generateItemJson, arrayUpdater, getElement().getNode());

    private int nextColumnId = 0;

    private GridSelectionModel<T> selectionModel = SelectionMode.SINGLE
            .createModel();

    public Grid() {
        dataCommunicator.setRequestedRange(0, pageSize);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        attachEvent.getUI().getPage().executeJavaScript(
                "window.gridConnector.initLazy($0, $1)", getElement(),
                pageSize);
    }

    /**
     * Adds a new text column to this {@link Grid} with a value provider. The
     * value is converted to a String using {@link Object#toString()}. In-memory
     * sorting will use the natural ordering of elements if they are mutually
     * comparable and otherwise fall back to comparing the string
     * representations of the values.
     *
     * @param header
     *            the column header name
     * @param valueProvider
     *            the value provider
     */
    public void addColumn(String header,
            ValueProvider<T, String> valueProvider) {
        int id = nextColumnId;
        nextColumnId++;
        String columnKey = "col" + id;
        columnGenerators.put(columnKey, valueProvider.andThen(Json::create));
        dataCommunicator.reset();

        // Use innerHTML to set document fragment instead of DOM children
        Element headerTemplate = new Element("template")
                .setAttribute("class", "header")
                .setProperty("innerHTML", HtmlUtils.escape(header));
        Element contentTemplate = new Element("template")
                .setProperty("innerHTML", "[[item." + columnKey + "]]");

        Element colElement = new Element("vaadin-grid-column")
                .setAttribute("id", columnKey)
                .appendChild(headerTemplate, contentTemplate);

        getElement().appendChild(colElement);
    }

    private JsonValue generateItemJson(String key, T item) {
        JsonObject json = Json.createObject();
        json.put("key", key);
        columnGenerators.forEach((columnKey, generator) -> json.put(columnKey,
                generator.apply(item)));
        return json;
    }

    @ClientDelegate
    public void confirmUpdate(int id) {
        dataCommunicator.confirmUpdate(id);
    }

    @ClientDelegate
    public void setRequestedRange(int start, int length) {
        dataCommunicator.setRequestedRange(start, length);
    }

    @Override
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        dataCommunicator.setDataProvider(dataProvider, null);
    }

    /**
     * Gets the current page size.
     *
     * @return the current page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size.
     *
     * @param pageSize
     *            the page size
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        dataCommunicator.setRequestedRange(0, pageSize);
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
     *
     * @see #setSelectionMode(SelectionMode)
     */
    protected void setSelectionModel(GridSelectionModel<T> model) {
        Objects.requireNonNull(model, "selection model cannot be null");
        selectionModel.remove();
        selectionModel = model;
    }

    /**
     * Sets the grid's selection mode.
     * <p>
     * To use your custom selection model, you can use
     * {@link #setSelectionModel(GridSelectionModel)}, see existing selection
     * model implementations for example.
     *
     * @param selectionMode
     *            the selection mode to switch to, not {@code null}
     * @return the used selection model
     *
     * @see SelectionMode
     * @see GridSelectionModel
     * @see #setSelectionModel(GridSelectionModel)
     */
    public GridSelectionModel<T> setSelectionMode(SelectionMode selectionMode) {
        Objects.requireNonNull(selectionMode, "Selection mode cannot be null.");
        GridSelectionModel<T> model = selectionMode.createModel();
        setSelectionModel(model);
        return model;
    }

    /**
     * Use this grid as a single select in {@link Binder}.
     * <p>
     * Throws {@link IllegalStateException} if the grid is not using a
     * {@link SingleSelectionModel}.
     *
     * @return the single select wrapper that can be used in binder
     * @throws IllegalStateException
     *             if not using a single selection model
     */
    public SingleSelect<? extends Grid<T>, T> asSingleSelect() {
        GridSelectionModel<T> model = getSelectionModel();
        if (!(model instanceof SingleSelectionModel)) {
            throw new IllegalStateException(
                    "Grid is not in single select mode, it needs to be explicitly set to such with setSelectionModel(SingleSelectionModel) before being able to use single selection features.");
        }
        return ((GridSingleSelectionModel<T>) model).asSingleSelect();
    }
}