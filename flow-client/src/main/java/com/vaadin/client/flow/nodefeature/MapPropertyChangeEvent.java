/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import com.vaadin.client.flow.reactive.ReactiveValueChangeEvent;

/**
 * Event fired when the value of a map property changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapPropertyChangeEvent extends ReactiveValueChangeEvent {

    private Object oldValue;
    private Object newValue;

    /**
     * Creates a new map property change event.
     *
     * @param source
     *            the changed map property
     * @param oldValue
     *            the old value
     * @param newValue
     *            the new value
     */
    public MapPropertyChangeEvent(MapProperty source, Object oldValue,
            Object newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the old property value.
     *
     * @return the old value
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new property value.
     *
     * @return the new value
     */
    public Object getNewValue() {
        return newValue;
    }

    @Override
    public MapProperty getSource() {
        return (MapProperty) super.getSource();
    }

}
