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
package com.vaadin.flow.component.webcomponent;

import java.util.EventObject;

/**
 * Event fired for a web component property value change.
 *
 * @param <T>
 *         type of the {@link WebComponentProperty} value
 */
public class PropertyValueChangeEvent<T> extends EventObject {

    private final T oldValue;
    private final T newValue;

    /**
     * PropertyValueChangeEvent constructor.
     *
     * @param source
     *         {@link WebComponentProperty} with the change
     * @param oldValue
     *         previous value
     * @param newValue
     *         new value
     */
    public PropertyValueChangeEvent(WebComponentProperty source, T oldValue,
            T newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public WebComponentProperty getSource() {
        return (WebComponentProperty) super.getSource();
    }

    /**
     * Previous value before the change.
     *
     * @return previous value
     */
    public T getOldValue() {
        return oldValue;
    }

    /**
     * New value after change.
     *
     * @return new value
     */
    public T getNewValue() {
        return newValue;
    }
}
