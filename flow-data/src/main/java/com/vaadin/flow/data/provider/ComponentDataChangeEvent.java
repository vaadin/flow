/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.data.provider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * An event fired when the filtering and/or sorting changes for a certain
 * component.
 * <p>
 * This event is thrown by {@link ListDataView} implementation when the
 * filtering and/or sorting changes through it's API and only limited to a
 * component bound to this {@link ListDataView} instance, in contrast with the
 * {@link DataChangeEvent}, which is thrown when the data changes in data
 * provider and the all components bound to that data provider are notified.
 * 
 * @param <C>
 *            the component to be notified about it's filtering and/or sorting
 *            changes
 */
public class ComponentDataChangeEvent<C extends Component>
        extends ComponentEvent<C> {

    /**
     * Creates a new component data change event.
     * 
     * @param source
     *            the component to be notified about it's filtering and/or
     *            sorting changes
     */
    public ComponentDataChangeEvent(C source) {
        super(source, false);
    }
}
