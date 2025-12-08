/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLayout;

public class RefreshCurrentRouteLayout implements RouterLayout {

    final static String ROUTER_LAYOUT_ID = "routerlayoutid";

    private Div layout = new Div();

    public RefreshCurrentRouteLayout() {
        final String uniqueId = UUID.randomUUID().toString();
        Div routerLayoutId = new Div(uniqueId);
        routerLayoutId.setId(ROUTER_LAYOUT_ID);
        layout.add(routerLayoutId);
    }

    @Override
    public Element getElement() {
        return layout.getElement();
    }
}
