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
package com.vaadin.cdi.itest.sessioncontextspecializes;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.VaadinSessionScoped;
import com.vaadin.cdi.itest.Counter;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

/**
 * Background-thread probe used by both strict-default and {@code @Specializes}
 * integration tests. From a thread that has only set the {@link VaadinSession}
 * thread-local — without acquiring the session lock — the view calls a method
 * on a {@code @VaadinSessionScoped} bean and fires a CDI event observed by the
 * same bean.
 * <p>
 * Outcomes are recorded via the application-scoped {@link Counter} so that
 * failures on the session-scoped proxy don't lose information:
 * {@link #DIRECT_CALL_COUNT} is incremented if the proxy call succeeds,
 * {@link #OBSERVED_COUNT} if the observer is invoked, and {@link #ERROR_COUNT}
 * for each {@link RuntimeException} caught by the background thread.
 * {@link #DONE_COUNT} is incremented when the background thread finishes,
 * regardless of outcome — tests synchronize on it before asserting the other
 * counters.
 */
@Route("")
@CdiComponent
public class SessionContextSpecializesView extends Div {

    public static final String FIREBTN_ID = "firebtn";
    public static final String OBSERVED_COUNT = "specializesObserved";
    public static final String DIRECT_CALL_COUNT = "specializesDirectCall";
    public static final String ERROR_COUNT = "specializesError";
    public static final String UNEXPECTED_ERROR_COUNT = "specializesUnexpectedError";
    public static final String DONE_COUNT = "specializesBackgroundDone";

    @Inject
    private SessionScopedObserver observer;

    @Inject
    private Event<BackgroundEvent> eventBus;

    @Inject
    private Counter counter;

    @PostConstruct
    private void init() {
        NativeButton fireBtn = new NativeButton("fire-background");
        fireBtn.addClickListener(click -> {
            VaadinSession session = VaadinSession.getCurrent();
            Thread thread = new Thread(() -> {
                VaadinSession previous = VaadinSession.getCurrent();
                VaadinSession.setCurrent(session);
                try {
                    try {
                        // Touch a @VaadinSessionScoped bean directly from a
                        // background thread that does NOT hold the session
                        // lock.
                        observer.recordDirectCall();
                    } catch (ContextNotActiveException e) {
                        counter.increment(ERROR_COUNT);
                    } catch (RuntimeException e) {
                        counter.increment(UNEXPECTED_ERROR_COUNT);
                    }
                    try {
                        // Fire a CDI event; the observer is on a
                        // @VaadinSessionScoped bean.
                        eventBus.fire(new BackgroundEvent());
                    } catch (ContextNotActiveException e) {
                        counter.increment(ERROR_COUNT);
                    } catch (RuntimeException e) {
                        counter.increment(UNEXPECTED_ERROR_COUNT);
                    }
                } finally {
                    VaadinSession.setCurrent(previous);
                    // Completion signal: tests wait for this counter before
                    // asserting the other ones.
                    counter.increment(DONE_COUNT);
                }
            }, "background-event-fire");
            thread.start();
        });
        fireBtn.setId(FIREBTN_ID);
        add(fireBtn);
    }

    public static class BackgroundEvent {
    }

    @VaadinSessionScoped
    public static class SessionScopedObserver {

        @Inject
        private Counter counter;

        public void recordDirectCall() {
            counter.increment(DIRECT_CALL_COUNT);
        }

        private void onBackgroundEvent(@Observes BackgroundEvent event) {
            counter.increment(OBSERVED_COUNT);
        }
    }
}
