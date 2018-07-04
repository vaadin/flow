/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.data.selection;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.flow.component.Component;

/**
 * A listener for listening for selection changes from a multiselection
 * component.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <C>
 *            the selection component type
 * @param <T>
 *            the type of the selected item
 *
 * @see SelectionModel.Multi
 * @see MultiSelectionEvent
 */
@FunctionalInterface
public interface MultiSelectionListener<C extends Component, T>
        extends Serializable, EventListener {

    /**
     * Invoked when the selection has changed by the user or programmatically.
     *
     * @param event
     *            the selection event, never {@code null}
     */
    void selectionChange(MultiSelectionEvent<C, T> event);
}
