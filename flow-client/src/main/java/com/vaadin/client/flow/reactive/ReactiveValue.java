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
package com.vaadin.client.flow.reactive;

import elemental.events.EventRemover;

/**
 * A reactive value fires reactive value change events when its value changes
 * and registers itself as dependent on the current computation when the value
 * is accessed.
 * <p>
 * A reactive value typically uses a {@link ReactiveEventRouter} for keeping
 * track of listeners, firing events and registering the value as dependent to
 * the current computation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ReactiveValue {
    /**
     * Adds a listener that has a dependency to this value, and should be
     * notified when this value changes.
     *
     * @param reactiveValueChangeListener
     *            the listener to add
     * @return an event remover that can be used for removing the added listener
     */
    EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener);
}
