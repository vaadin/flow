/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.cdi.itest.routecontext;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "layout-event", layout = EventObserverLayout.class)
@RouteScoped
@CdiComponent
public class LayoutEventView extends Div {

    public static final String FIRE = "FIRE";
    public static final String CHANGE_VIEW = "CHANGE_VIEW";
    public static final String VIEW_NAME = "VIEW_NAME";
    @Inject
    private Event<EventObserverLayout.CustomEvent> eventTrigger;

    @PostConstruct
    private void init() {
        NativeButton fireBtn = new NativeButton("fire event",
                clickEvent -> eventTrigger
                        .fire(EventObserverLayout.CustomEvent.INSTANCE));
        fireBtn.setId(FIRE);
        NativeButton navigateBtn = new NativeButton("change view",
                clickEvent -> UI.getCurrent().navigate(LayoutEventView2.class));
        navigateBtn.setId(CHANGE_VIEW);

        Span viewName = new Span("Layout Event View 1");
        viewName.setId(VIEW_NAME);
        add(viewName, fireBtn, navigateBtn);
    }

}
