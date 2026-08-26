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
package com.vaadin.flow.uitest.ui.routerstate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

@Route(value = "com.vaadin.flow.uitest.ui.routerstate.RouterStateAView", layout = RouterStateLayout.class)
public class RouterStateAView extends Div {

    public static final String LINK_TO_B = "router-state-link-b";
    public static final String LINK_TO_B_WITH_ID = "router-state-link-b-id";

    public RouterStateAView() {
        add(new H2("View A"));

        RouterLink linkB = new RouterLink("Go to B", RouterStateBView.class);
        linkB.setId(LINK_TO_B);

        RouterLink linkBId = new RouterLink("Go to B/42",
                RouterStateBView.class, new RouteParameters("id", "42"));
        linkBId.setId(LINK_TO_B_WITH_ID);

        add(linkB, new Div(), linkBId);
    }
}
