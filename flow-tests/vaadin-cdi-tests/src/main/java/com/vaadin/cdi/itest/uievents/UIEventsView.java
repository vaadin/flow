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
package com.vaadin.cdi.itest.uievents;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.EventObject;
import java.util.List;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.UIScoped;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;

@Route("uievents")
@UIScoped
@CdiComponent
public class UIEventsView extends Div implements AfterNavigationObserver {

    public static final String POLL_FROM_CLIENT = "POLL_FROM_CLIENT";
    public static final String NAVIGATION_EVENTS = "NAVIGATION_EVENTS";

    @Inject
    private NavigationObserver navigationObserver;

    @PostConstruct
    private void init() {
        UI.getCurrent().setPollInterval(500);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        showNavigationEvents();
    }

    private void showNavigationEvents() {
        Div events = new Div();
        events.setId(NAVIGATION_EVENTS);
        List<EventObject> navigationEvents = navigationObserver
                .getNavigationEvents();
        navigationEvents.stream()
                .map(event -> new NativeLabel(event.getClass().getSimpleName()))
                .forEach(events::add);
        add(events);
    }

    private void showPollEvent(@Observes PollEvent pollEvent) {
        final NativeLabel poll = new NativeLabel(pollEvent.isFromClient() + "");
        poll.setId(POLL_FROM_CLIENT);
        add(new Div(poll));
    }
}
