/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.NavigationView")
public class NavigationView extends Div {

    public static final String SERVER_ID = "server-navigation";
    public static final String ANCHOR_ID = "anchor-navigation";
    public static final String ROUTER_LINK_ID = "router-link-navigation";
    public static final String POSTPONE_ID = "postpone-view-link";

    public NavigationView() {
        Anchor anchorNavigation = new Anchor("com.vaadin.flow.AnchorView",
                "Navigate to AnchorView");
        anchorNavigation.setId(ANCHOR_ID);
        NativeButton serverNavigation = new NativeButton(
                "Navigate through Server", event -> {
                    event.getSource().getUI().get().navigate(ServerView.class);
                });
        serverNavigation.setId(SERVER_ID);
        RouterLink link = new RouterLink("RouterView", RouterView.class);
        link.setId(ROUTER_LINK_ID);

        RouterLink postponeView = new RouterLink("PostponeView",
                PostponeView.class);
        postponeView.setId(POSTPONE_ID);

        add(new Span("NavigationView"), new Div(), anchorNavigation, new Div(),
                serverNavigation, new Div(), link, new Div(), postponeView);
    }

}
