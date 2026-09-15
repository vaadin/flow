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
package com.vaadin.flow.quarkus.it.uievents;

import java.util.EventObject;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.quarkus.annotation.NormalUIScoped;

@Route("uievents")
public class UIEventsView extends Div implements AfterNavigationObserver {

    public static final String POLL_FROM_CLIENT = "POLL_FROM_CLIENT";
    public static final String NAVIGATION_EVENTS = "NAVIGATION_EVENTS";

    @NormalUIScoped
    public static class PollObserver {
        private void showPollEvent(@Observes PollEvent pollEvent) {
            UI ui = pollEvent.getSource();

            List<HasElement> chain = ui.getInternals()
                    .getActiveRouterTargetsChain();

            HasElement leaf = chain.get(chain.size() - 1);
            if (leaf instanceof UIEventsView) {

                final Span poll = new Span(pollEvent.isFromClient() + "");
                poll.setId(POLL_FROM_CLIENT);

                ((UIEventsView) leaf).add(new Div(poll));
            }
        }
    }

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
                .map(event -> new Span(event.getClass().getSimpleName()))
                .forEach(events::add);
        add(events);
    }

}
