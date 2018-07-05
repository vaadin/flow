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
package com.vaadin.flow.router.internal;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.Router;

/**
 * Handles navigation by redirecting the user to some location in the
 * application.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class InternalRedirectHandler implements NavigationHandler {
    private final Location target;

    /**
     * Creates a new redirect handler for the provided location.
     *
     * @param target
     *            the target of the redirect, not <code>null</code>
     */
    public InternalRedirectHandler(Location target) {
        assert target != null;
        this.target = target;
    }

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();
        Router router = event.getSource();

        ui.getPage().getHistory().replaceState(null, target);

        return router.navigate(ui, target, event.getTrigger());
    }
}
