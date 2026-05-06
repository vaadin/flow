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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

@Route(value = "com.vaadin.flow.uitest.ui.routerstate.RouterStateBView/:id?", layout = RouterStateLayout.class)
public class RouterStateBView extends Div {

    public static final String LINK_TO_A = "router-state-link-a";
    public static final String SET_LOCALE_BUTTON = "router-state-set-locale";

    public RouterStateBView() {
        add(new H2("View B"));

        RouterLink linkA = new RouterLink("Go to A", RouterStateAView.class);
        linkA.setId(LINK_TO_A);

        // Self-navigation to a different :id parameter — verifies that the
        // signal updates also for navigations within the same view class.
        RouterLink linkSelf99 = new RouterLink("Go to B/99",
                RouterStateBView.class, new RouteParameters("id", "99"));
        linkSelf99.setId("router-state-link-b-id-99");

        // Programmatic locale change as an unrelated UI mutation — verifies
        // that the router signal does NOT fire on non-navigation changes.
        NativeButton setLocale = new NativeButton("Change UI locale",
                e -> e.getSource().getUI().ifPresent(
                        ui -> ui.setLocale(java.util.Locale.GERMAN)));
        setLocale.setId(SET_LOCALE_BUTTON);

        add(linkA, new Div(), linkSelf99, new Div(), setLocale);
    }
}
