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
