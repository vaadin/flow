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
package com.vaadin.ui.common;

import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.ComponentEventNotifier;
import com.vaadin.ui.event.DetachEvent;

/**
 * Mixin interface for components that support adding detach listeners.
 *
 * @author Vaadin Ltd
 */
public interface DetachNotifier extends ComponentEventNotifier {

    /**
     * Adds a detach listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addDetachListener(
            ComponentEventListener<DetachEvent> listener) {
        return addListener(DetachEvent.class, listener);
    }
}
