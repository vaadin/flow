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
package com.vaadin.flow.component.page;

import java.io.Serializable;

/**
 * Listener that gets notified when the size of the browser window containing
 * the uI has changed.
 *
 * @author Vaadin Ltd
 * @since 1.2
 *
 * @see Page#addBrowserWindowResizeListener(BrowserWindowResizeListener)
 */
@FunctionalInterface
public interface BrowserWindowResizeListener extends Serializable {
    /**
     * Invoked when the browser window containing a UI has been resized.
     *
     * @param event
     *            a browser window resize event
     */
    void browserWindowResized(BrowserWindowResizeEvent event);
}
