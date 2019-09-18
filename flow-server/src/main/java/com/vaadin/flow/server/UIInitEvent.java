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

import java.util.EventObject;

import com.vaadin.flow.component.UI;

/**
 * Event fired to {@link UIInitListener} when a {@link UI} has been initialized.
 *
 * @since 1.0
 */
public class UIInitEvent extends EventObject {

    private final UI ui;

    /**
     * Constructs a prototypical Event.
     *
     * @param service
     *            the service from which the event originates
     * @param ui
     *            the initialized UI
     */
    public UIInitEvent(UI ui, VaadinService service) {
        super(service);
        this.ui = ui;
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    /**
     * Get the initialized UI for this initialization event.
     * 
     * @return initialized UI
     */
    public UI getUI() {
        return ui;
    }
}
