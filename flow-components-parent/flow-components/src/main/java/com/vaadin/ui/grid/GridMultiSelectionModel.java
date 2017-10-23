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

import com.vaadin.data.Binder;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.selection.MultiSelect;
import com.vaadin.data.selection.MultiSelectionListener;
import com.vaadin.data.selection.SelectionModel;
import com.vaadin.shared.Registration;

/**
 * Multiselection model interface for Grid.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the type of items in grid
 */
public interface GridMultiSelectionModel<T>
        extends GridSelectionModel<T>, SelectionModel.Multi<T> {

    /**
     * State for showing the select all checkbox in the grid's default header
     * row for the selection column.
     * <p>
     * Default value is {@link #DEFAULT}, which means that the select all is
     * only visible if an in-memory data provider is used
     * {@link DataProvider#isInMemory()}.
     */
    public enum SelectAllCheckboxVisibility {

        /**
         * Shows the select all checkbox, regardless of data provider used.
         * <p>
         * <b>For a lazy data provider, selecting all will result in to all rows
         * being fetched from backend to application memory!</b>
         */
        VISIBLE,

        /**
         * Never shows the select all checkbox, regardless of data provider
         * used.
         */
        HIDDEN,

        /**
         * By default select all checkbox depends on the grid's dataprovider.
         * <ul>
         * <li>Visible, if the data provider is in-memory</li>
         * <li>Hidden, if the data provider is NOT in-memory (lazy)</li>
         * </ul>
         *
         * @see DataProvider#isInMemory()}.
         */
        DEFAULT;
    }

    /**
     * Gets a wrapper to use this multiselection model as a multiselect in
     * {@link Binder}.
     *
     * @return the multiselect wrapper
     */
    MultiSelect<Grid<T>, T> asMultiSelect();

    /**
     * Adds a selection listener that will be called when the selection is
     * changed either by the user or programmatically.
     *
     * @param listener
     *            the multi selection listener, not {@code null}
     * @return a registration for the listener
     */
    Registration addMultiSelectionListener(
            MultiSelectionListener<Grid<T>, T> listener);

    /**
     * Sets the select all checkbox visibility mode.
     * <p>
     * The default value is {@link SelectAllCheckboxVisibility#DEFAULT}, which
     * means that the checkbox is only visible if the grid's data provider is
     * in- memory.
     *
     * @param selectAllCheckBoxVisibility
     *            the visiblity mode to use
     * @see SelectAllCheckboxVisibility
     */
    void setSelectAllCheckboxVisibility(
            SelectAllCheckboxVisibility selectAllCheckBoxVisibility);

    /**
     * Gets the current mode for the select all checkbox visibility.
     *
     * @return the select all checkbox visibility mode
     * @see SelectAllCheckboxVisibility
     * @see #isSelectAllCheckboxVisible()
     */
    SelectAllCheckboxVisibility getSelectAllCheckboxVisibility();

    /**
     * Returns whether the select all checkbox will be visible with the current
     * setting of
     * {@link #setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility)}.
     *
     * @return {@code true} if the checkbox will be visible with the current
     *         settings
     * @see SelectAllCheckboxVisibility
     * @see #setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility)
     */
    boolean isSelectAllCheckboxVisible();
}
