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
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Event listener that can be registered for receiving an event when a
 * {@link com.vaadin.flow.component.UI} is initialized.
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
