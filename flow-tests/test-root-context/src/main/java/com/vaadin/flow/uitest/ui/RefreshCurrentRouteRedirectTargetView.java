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
package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectTargetView", layout = RefreshCurrentRouteLayout.class)
public class RefreshCurrentRouteRedirectTargetView extends Div {

    static final String VIEW_ID = "forward-target-id";

    public RefreshCurrentRouteRedirectTargetView() {
        Div id = new Div(UUID.randomUUID().toString());
        id.setId(VIEW_ID);
        add(id);
    }
}
