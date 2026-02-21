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
package com.vaadin.flow.component.page;

import java.util.EventObject;

/**
 * Event that is fired when the fullscreen state of the page changes.
 *
 * @author Vaadin Ltd
 *
 * @see FullscreenChangeListener
 * @see Page#addFullscreenChangeListener(FullscreenChangeListener)
 */
public class FullscreenChangeEvent extends EventObject {

    private final boolean fullscreen;

    /**
     * Creates a new event.
     *
     * @param source
     *            the page for which the fullscreen state has changed
     * @param fullscreen
     *            {@code true} if the page is now in fullscreen mode,
     *            {@code false} if fullscreen was exited
     */
    public FullscreenChangeEvent(Page source, boolean fullscreen) {
        super(source);
        this.fullscreen = fullscreen;
    }

    @Override
    public Page getSource() {
        return (Page) super.getSource();
    }

    /**
     * Returns whether the page is currently in fullscreen mode.
     *
     * @return {@code true} if in fullscreen mode, {@code false} otherwise
     */
    public boolean isFullscreen() {
        return fullscreen;
    }
}
