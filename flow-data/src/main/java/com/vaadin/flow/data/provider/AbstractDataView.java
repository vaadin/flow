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
import com.vaadin.flow.shared.Registration;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract data view implementation which takes care of processing
 * component data size change events.
 *
 * @param <T>
 *        data type
 * @param <F>
 *        filter type
 * @param <C>
 *        component type
 */
public abstract class AbstractDataView<T, F, C extends Component> implements DataView<T, F>,
        SizeChangeHandler {

    protected int filteredItemsSize = 0;
    protected C component;
    protected Set<SizeChangeListener> sizeChangeListeners;

    public AbstractDataView(C component) {
        this.component = component;
    }

    @Override
    public Registration addSizeChangeListener(SizeChangeListener listener) {
        if (sizeChangeListeners == null) {
            sizeChangeListeners = new HashSet<>();
        }
        sizeChangeListeners.add(listener);
        return () -> sizeChangeListeners.remove(listener);
    }

    @Override
    public void sizeEvent(int size) {
        if (size != filteredItemsSize && sizeChangeListeners != null) {
            sizeChangeListeners.forEach(listener -> listener.sizeChanged(
                    new SizeChangeEvent<>(component, size)));
        }
        filteredItemsSize = size;
    }
}
