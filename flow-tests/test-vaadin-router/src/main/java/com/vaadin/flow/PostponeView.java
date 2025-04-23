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

package com.vaadin.flow;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.PostponeView")
public class PostponeView extends Div implements BeforeLeaveObserver {

    public static String CONTINUE_ID = "continue-button";
    public static String STAY_ID = "stay-button";
    public static String NAVIGATION_ID = "anchor-to-navigation";
    public static String NAVIGATION_ROUTER_LINK_ID = "routerlink-to-navigation";

    private NativeButton navigate, stay;

    public PostponeView() {
        Anchor link = new Anchor("com.vaadin.flow.NavigationView",
                "Navigation");// NavigationView.class);
        link.setId(NAVIGATION_ID);
        RouterLink routerLink = new RouterLink("Navigation",
                NavigationView.class);
        routerLink.setId(NAVIGATION_ROUTER_LINK_ID);

        add(new Span("PostponeView"), new Div(), link, new Div(), routerLink);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        BeforeLeaveEvent.ContinueNavigationAction postpone = event.postpone();
        navigate = new NativeButton("Continue", e -> {
            postpone.proceed();
            remove(navigate, stay);
        });
        navigate.setId(CONTINUE_ID);
        stay = new NativeButton("Stay", e -> {
            postpone.cancel();
            remove(navigate, stay);
        });
        stay.setId(STAY_ID);

        add(navigate, stay);
    }
}
