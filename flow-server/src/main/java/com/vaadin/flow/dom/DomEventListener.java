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
package com.vaadin.flow.dom;

import java.io.Serializable;

/**
 * Server-side listener for client-side DOM events.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface DomEventListener extends Serializable {
    /**
     * Invoked when a DOM event has been fired.
     *
     * @param event
     *            the fired event
     */
    void handleEvent(DomEvent event);
}
