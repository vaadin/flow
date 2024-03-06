/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.reactive;

import elemental.events.EventRemover;

public class TestReactiveEventRouter extends
        ReactiveEventRouter<ReactiveValueChangeListener, ReactiveValueChangeEvent> {

    private static final class TestEventSource implements ReactiveValue {
        private ReactiveEventRouter<ReactiveValueChangeListener, ReactiveValueChangeEvent> router;

        @Override
        public EventRemover addReactiveValueChangeListener(
                ReactiveValueChangeListener reactiveValueChangeListener) {
            return router.addReactiveListener(reactiveValueChangeListener);
        }
    }

    public TestReactiveEventRouter() {
        super(new TestEventSource());
        ((TestEventSource) getReactiveValue()).router = this;
    }

    @Override
    protected ReactiveValueChangeListener wrap(ReactiveValueChangeListener l) {
        return l;
    }

    @Override
    protected void dispatchEvent(
            ReactiveValueChangeListener reactiveValueChangeListener,
            ReactiveValueChangeEvent event) {
        reactiveValueChangeListener.onValueChange(event);
    }

    public void invalidate() {
        fireEvent(new ReactiveValueChangeEvent(getReactiveValue()) {
            // Nothing interesting here
        });
    }
}
