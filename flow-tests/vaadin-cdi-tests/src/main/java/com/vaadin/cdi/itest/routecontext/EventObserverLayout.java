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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;

import java.time.LocalDateTime;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLayout;

@RouteScoped
@CdiComponent
public class EventObserverLayout extends Div implements RouterLayout {

    public static final String EVENTS_COUNTER_LABEL = "eventsCounter";
    private final NativeLabel receivedEvents;
    private int counter = 0;

    public EventObserverLayout() {
        receivedEvents = new NativeLabel();
        receivedEvents.setId(EVENTS_COUNTER_LABEL);
        add(receivedEvents);
    }

    public void onEvent(
            @Observes(notifyObserver = Reception.IF_EXISTS) CustomEvent event) {
        receivedEvents.setText("EVENTS COUNT: " + ++counter);
        add(new Span("EVENT AT " + LocalDateTime.now()));
    }

    public enum CustomEvent {
        INSTANCE
    }

}
