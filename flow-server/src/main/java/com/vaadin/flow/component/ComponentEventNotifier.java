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
package com.vaadin.flow.component;

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * Something that you can add component event listeners to. This interface
 * defines a generic <code>addListener</code> method that an extending interface
 * can use in a default implementation for adding a listener for a specific
 * event type.
 *
 * @author Vaadin Ltd
 */
public interface ComponentEventNotifier extends Serializable {
    /**
     * Adds a listener for an event of the given type.
     *
     * @param <T>
     *            the component event type
     * @param eventType
     *            the component event type, not <code>null</code>
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    <T extends ComponentEvent<?>> Registration addListener(
            Class<T> eventType, ComponentEventListener<T> listener);
}
