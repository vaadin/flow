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
 * A listener for {@code SelectionEvent}.
 * <p>
 * This is a generic listener for both type of selections, single and
 * multiselect.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the type of the selected item
 * @param <C>
 *            the component type
 *
 * @see SelectionEvent
 */
@FunctionalInterface
public interface SelectionListener<C extends Component, T>
        extends Serializable, EventListener {

    /**
     * Invoked when the selection has changed.
     *
     * @param event
     *            the selection event
     */
    void selectionChange(SelectionEvent<C, T> event);
}
