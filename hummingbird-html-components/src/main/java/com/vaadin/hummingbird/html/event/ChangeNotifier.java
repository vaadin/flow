/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.html.event;

import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.event.ComponentEventListener;
import com.vaadin.ui.ComponentEventNotifier;

/**
 * Mixin interface for components that support adding change listeners to the
 * their root elements.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface ChangeNotifier extends ComponentEventNotifier {
    /**
     * Adds a change listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default EventRegistrationHandle addChangeListener(
            ComponentEventListener<ChangeEvent> listener) {
        return addListener(ChangeEvent.class, listener);
    }
}
