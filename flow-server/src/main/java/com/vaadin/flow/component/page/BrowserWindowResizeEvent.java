/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.page;

import java.util.EventObject;

/**
 * Event that is fired when a browser window containing a uI is resized.
 *
 * @author Vaadin Ltd
 * @since 1.2
 *
 * @see BrowserWindowResizeListener
 */
public class BrowserWindowResizeEvent extends EventObject {

    private final int width;
    private final int height;

    /**
     * Creates a new event.
     *
     * @param source
     *            the page for which the browser window has been resized
     * @param width
     *            the new width of the browser window
     * @param height
     *            the new height of the browser window
     */
    public BrowserWindowResizeEvent(Page source, int width, int height) {
        super(source);
        this.width = width;
        this.height = height;
    }

    @Override
    public Page getSource() {
        return (Page) super.getSource();
    }

    /**
     * Gets the new browser window height.
     *
     * @return an integer with the new pixel height of the browser window
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the new browser window width.
     *
     * @return an integer with the new pixel width of the browser window
     */
    public int getWidth() {
        return width;
    }

}
