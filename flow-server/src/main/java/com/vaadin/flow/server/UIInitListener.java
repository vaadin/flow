/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Event listener that can be registered for receiving an event when a
 * {@link com.vaadin.flow.component.UI} is initialized.
 * <p>
 * An exception thrown by a listener is logged and the remaining listeners are
 * notified regardless, so a failing listener does not stop the UI from being
 * initialized. A listener that has to prevent the UI from being used has to say
 * so through the UI itself.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface UIInitListener extends Serializable {

    /**
     * Notifies when UI is initialized .
     *
     * @param event
     *            event for the initialization
     */
    void uiInit(UIInitEvent event);
}
