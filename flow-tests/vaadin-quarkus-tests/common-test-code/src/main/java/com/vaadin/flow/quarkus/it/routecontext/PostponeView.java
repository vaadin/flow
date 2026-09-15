/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.routecontext;

import jakarta.annotation.PostConstruct;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.quarkus.annotation.RouteScoped;

@Route("postpone")
@RouteScoped
public class PostponeView extends AbstractCountedView
        implements BeforeLeaveObserver {

    public static final String NAVIGATE = "NAVIGATE";
    public static final String POSTPONED_ROOT = "postpone";

    private BeforeLeaveEvent.ContinueNavigationAction navigationAction;

    @PostConstruct
    private void init() {
        NativeButton navBtn = new NativeButton("navigate",
                clickEvent -> navigationAction.proceed());
        navBtn.setId(NAVIGATE);

        add(new Div(new RouterLink(POSTPONED_ROOT, RootView.class)),
                new Div(navBtn));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent beforeLeaveEvent) {
        navigationAction = beforeLeaveEvent.postpone();
    }

}
