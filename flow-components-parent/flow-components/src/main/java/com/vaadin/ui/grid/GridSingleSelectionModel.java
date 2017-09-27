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
import com.vaadin.data.selection.SelectionModel;
import com.vaadin.data.selection.SingleSelect;
import com.vaadin.data.selection.SingleSelectionListener;
import com.vaadin.shared.Registration;

/**
 * Single selection model interface for Grid.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the type of items in grid
 */
public interface GridSingleSelectionModel<T>
        extends GridSelectionModel<T>, SelectionModel.Single<T> {

    /**
     * Gets a wrapper to use this single selection model as a single select in
     * {@link Binder}.
     *
     * @return the single select wrapper
     */
    SingleSelect<Grid<T>, T> asSingleSelect();

    /**
     * Adds a selection listener that will be called when the selection is
     * changed either by the user or programmatically.
     *
     * @param listener
     *            the single selection listener, not {@code null}
     * @return a registration for the listener
     */
    Registration addSingleSelectionListener(
            SingleSelectionListener<Grid<T>, T> listener);
}
