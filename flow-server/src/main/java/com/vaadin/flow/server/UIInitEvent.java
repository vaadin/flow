/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;

import java.util.EventObject;

/**
 * Event gets fired when a new {@link com.vaadin.flow.component.UI} is initialized.
 *
 * @see UIInitListener#uiInit(UIInitEvent)
 *
 * @author Vaadin Ltd
 */
public class UIInitEvent extends EventObject {

    private final UI ui;

    /**
     * Creates a new event.
     *
     * @param service
     *            the Vaadin service from which the event originates
     * @param ui the UI that has been initialized
     */
    public UIInitEvent(UI ui, VaadinService service) {
        super(service);
        this.ui = ui;
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    public UI getUi() {
        return ui;
    }
}
