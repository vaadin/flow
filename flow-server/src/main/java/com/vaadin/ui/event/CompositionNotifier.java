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
 * Mixin interface for components that support adding composition listeners to
 * the their root elements.
 *
 * See <a href=
 * "https://developer.mozilla.org/docs/Web/API/CompositionEvent">CompositionEvent</a>
 *
 * @author Vaadin Ltd
 */
public interface CompositionNotifier extends ComponentEventNotifier {

    /**
     * Adds a {@code compositionstart} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addCompositionStartListener(
            ComponentEventListener<CompositionStartEvent> listener) {
        return addListener(CompositionStartEvent.class, listener);
    }

    /**
     * Adds a {@code compositionupdate} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addCompositionUpdateListener(
            ComponentEventListener<CompositionUpdateEvent> listener) {
        return addListener(CompositionUpdateEvent.class, listener);
    }

    /**
     * Adds a {@code compositionend} listener to this component.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    default Registration addCompositionEndListener(
            ComponentEventListener<CompositionEndEvent> listener) {
        return addListener(CompositionEndEvent.class, listener);
    }

}
