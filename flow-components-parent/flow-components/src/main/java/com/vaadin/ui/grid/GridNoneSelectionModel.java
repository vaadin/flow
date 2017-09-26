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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @param <T>
 */
public class GridNoneSelectionModel<T> implements GridSelectionModel<T> {

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
    public void remove() {
    }

    @Override
    public void selectFromClient(T item) {
        throw new IllegalStateException("Client tried to update selection"
                + " even though selection mode is currently set to NONE.");
    }

    @Override
    public void deselectFromClient(T item) {
        throw new IllegalStateException("Client tried to update selection"
                + " even though selection mode is currently set to NONE.");
    }
}
