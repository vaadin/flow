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

import java.util.EventObject;

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.UI;

/**
 * Event fired to {@link BrowserTabInitListener} when a new {@link BrowserTab}
 * has been created.
 */
@NullMarked
public class BrowserTabInitEvent extends EventObject {

    private final BrowserTab browserTab;

    private final UI ui;

    /**
     * Creates a new event.
     *
     * @param browserTab
     *            the created browser tab, not {@code null}
     * @param ui
     *            the UI that the browser tab was created for, not {@code null}
     * @param service
     *            the service from which the event originates, not {@code null}
     */
    public BrowserTabInitEvent(BrowserTab browserTab, UI ui,
            VaadinService service) {
        super(service);
        this.browserTab = browserTab;
        this.ui = ui;
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    /**
     * Gets the created browser tab.
     *
     * @return the created browser tab, not {@code null}
     */
    public BrowserTab getBrowserTab() {
        return browserTab;
    }

    /**
     * Gets the UI that the browser tab was created for. This is the first UI
     * loaded in the browser tab that used it.
     *
     * @return the UI of the browser tab, not {@code null}
     */
    public UI getUI() {
        return ui;
    }
}
