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
 * Event listener that can be registered for receiving an event when a new
 * {@link BrowserTab} has been created.
 * <p>
 * A browser tab is created when the first UI is loaded in a browser tab, before
 * any route target or layout of that UI is created and before the
 * {@link UIInitListener}s are notified. Reloading the page or navigating within
 * the same browser tab does not create a new browser tab, which makes this the
 * place for initializing the state of a browser tab and registering its cleanup
 * with {@link BrowserTab#addDestroyListener}.
 *
 * @see VaadinService#addBrowserTabInitListener(BrowserTabInitListener)
 */
@FunctionalInterface
public interface BrowserTabInitListener extends Serializable {

    /**
     * Notifies when a browser tab has been created.
     *
     * @param event
     *            event for the initialization
     */
    void browserTabInit(BrowserTabInitEvent event);
}
