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
