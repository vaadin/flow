/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.reactive;

import elemental.events.EventRemover;

public class TestReactiveEventRouter extends
        ReactiveEventRouter<ReactiveChangeListener, ReactiveChangeEvent> {

    private static final class TestEventSource implements ReactiveValue {
        private ReactiveEventRouter<ReactiveChangeListener, ReactiveChangeEvent> router;

        @Override
        public EventRemover addReactiveChangeListener(
                ReactiveChangeListener listener) {
            return router.addReactiveListener(listener);
        }
    }

    public TestReactiveEventRouter() {
        super(new TestEventSource());
        ((TestEventSource) getReactiveValue()).router = this;
    }

    @Override
    protected ReactiveChangeListener wrap(ReactiveChangeListener l) {
        return l;
    }

    @Override
    protected void dispatchEvent(ReactiveChangeListener listener,
            ReactiveChangeEvent event) {
        listener.onChange(event);
    }

    public void invalidate() {
        fireEvent(new ReactiveChangeEvent(getReactiveValue()) {
            // Nothing interesting here
        });
    }
}
