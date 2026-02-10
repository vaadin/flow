/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;

public class ValueChangeMonitor<T> {
    public ValueChangeMonitor(HasValue<?, T> obserable) {
        obserable.addValueChangeListener(event -> {
            if (capturedEvent != null) {
                Assertions.fail("There is already an event. Old event: "
                        + capturedEvent + ", new event: " + event);
            }

            Assertions.assertSame(obserable, event.getHasValue());

            if (event instanceof ComponentValueChangeEvent<?, ?>) {
                Assertions.assertSame(obserable,
                        ((ComponentValueChangeEvent<?, ?>) event).getSource());
            }

            capturedEvent = event;
        });
    }

    ValueChangeEvent<T> capturedEvent;

    public void discard() {
        Assertions.assertNotNull(capturedEvent, "There should be an event");
        capturedEvent = null;
    }

    public void assertEvent(boolean fromClient, T oldValue, T newValue) {
        Assertions.assertNotNull(capturedEvent, "There should be an event");
        Assertions.assertTrue(fromClient == capturedEvent.isFromClient());

        assertEventValues(capturedEvent, oldValue, newValue);

        discard();
    }

    public void assertNoEvent() {
        Assertions.assertNull(capturedEvent, "There should be no event");
    }

    public static <T> void assertEventValues(ValueChangeEvent<T> event,
            T oldValue, T newValue) {
        Assertions.assertEquals(oldValue, event.getOldValue());
        Assertions.assertEquals(newValue, event.getValue());
    }
}
