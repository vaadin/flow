/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
