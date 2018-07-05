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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.DebouncePhase;

/**
 * Debounce settings for declaratively defined client-side event handlers.
 *
 * @see DomEvent
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public @interface DebounceSettings {
    /**
     * Gets the debounce timeout to use.
     *
     * @return the debounce timeout in milliseconds, or 0 to disable debouncing.
     */
    int timeout();

    /**
     * Gets an array of debounce phases for which the event should be sent to
     * the server. There must be at least one phase.
     *
     * @see DebouncePhase
     *
     * @return an array of debounce phases
     */
    DebouncePhase[] phases();
}
