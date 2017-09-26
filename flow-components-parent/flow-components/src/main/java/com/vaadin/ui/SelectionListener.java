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

import com.vaadin.ui.event.ComponentEventListener;

/**
 * A listener for {@code SelectionEvent}.
 * <p>
 * This is a generic listener for both type of selections, single and
 * multiselect.
 *
 * @author Vaadin Ltd.
 *
 * @param <L>
 *            the type of the listing component
 * @param <T>
 *            the type of the selected item
 *
 * @see SelectionEvent
 */
@FunctionalInterface
public interface SelectionListener<L extends AbstractListing<T>, T>
        extends ComponentEventListener<SelectionEvent<L, T>> {

    /**
     * Invoked when the selection has changed by user or programmatically.
     *
     * @param event
     *            the selection event
     */
    @Override
    public void onComponentEvent(SelectionEvent<L, T> event);
}
