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
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.quarkus.annotation.RouteScopeOwner;
import com.vaadin.quarkus.annotation.RouteScoped;

@Route("event")
@RouteScoped
public class EventView extends Div {

    public static final String FIRE = "FIRE";
    public static final String OBSERVER_LABEL = "OBSERVER_LABEL";

    @RouteScoped
    @RouteScopeOwner(EventView.class)
    public static class ObserverLabel extends Span {
        private void onPrintEvent(@Observes PrintEvent printEvent) {
            setText(printEvent.getMessage());
        }
    }

    public static class PrintEvent {
        private final String message;

        public PrintEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Inject
    @RouteScopeOwner(EventView.class)
    private Span label;
    @Inject
    private Event<PrintEvent> printEventTrigger;

    @PostConstruct
    private void init() {
        label.setId(OBSERVER_LABEL);
        NativeButton fireBtn = new NativeButton("fire event",
                clickEvent -> printEventTrigger.fire(new PrintEvent("HELLO")));
        fireBtn.setId(FIRE);

        add(fireBtn, label);
    }

}
