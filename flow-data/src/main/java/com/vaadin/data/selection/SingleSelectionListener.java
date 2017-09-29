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
package com.vaadin.data.selection;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.data.AbstractListing;

/**
 * A listener for listening to selection changes on a single selection
 * component.
 *
 * @author Vaadin Ltd.
 *
 * @param <L>
 *            the listing component type
 * @param <T>
 *            the type of the selected item
 *
 * @see SelectionModel.Single
 * @see SingleSelectionEvent
 */
@FunctionalInterface
public interface SingleSelectionListener<L extends AbstractListing<T>, T>
        extends Serializable, EventListener {

    /**
     * Invoked when selection has been changed.
     *
     * @param event
     *            the single selection event
     */
    void selectionChange(SingleSelectionEvent<L, T> event);
}
