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

import java.io.Serializable;

/**
 * Listener for receiving changes to {@link WebComponentProperty} value.
 *
 * @param <T>
 *         type of the {@link WebComponentProperty} value
 */
@FunctionalInterface
public interface PropertyValueChangeListener<T> extends Serializable {

    /**
     * Method called when target property value has changed.
     *
     * @param event
     *         property value change event
     */
    void valueChange(PropertyValueChangeEvent<T> event);
}
