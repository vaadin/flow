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
package com.vaadin.ui.event;

import com.vaadin.shared.Registration;

/**
 * Mixin interface for components that support adding key event listeners to the
 * their root elements.
 *
 * @author Vaadin Ltd
 */
public interface KeyUpNotifier extends ComponentEventNotifier {

    /**
     * Adds a keyup listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyUpListener(
            ComponentEventListener<KeyUpEvent> listener) {
        return addListener(KeyUpEvent.class, listener);
    }

    /**
     * Adds a keyup listener to this component, conditional on key and
     * modifiers.
     *
     * @param key
     *            the key to match
     * @param listener
     *            the listener to add, not <code>null</code>
     * @param modifiers
     *            the modifiers to match
     * @return a handle that can be used for removing the listener
     */
    default Registration addKeyUpListener(Key key,
            ComponentEventListener<KeyUpEvent> listener,
            KeyModifier... modifiers) {
        return addKeyUpListener(
                new KeyEventListener<>(listener, key, modifiers));
    }

}
