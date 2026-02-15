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

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ValueChangeMonitor<T> {
    public ValueChangeMonitor(HasValue<?, T> obserable) {
        obserable.addValueChangeListener(event -> {
            if (capturedEvent != null) {
                fail("There is already an event. Old event: " + capturedEvent
                        + ", new event: " + event);
            }

            assertSame(obserable, event.getHasValue());

            if (event instanceof ComponentValueChangeEvent<?, ?>) {
                assertSame(obserable,
                        ((ComponentValueChangeEvent<?, ?>) event).getSource());
            }

            capturedEvent = event;
        });
    }

    ValueChangeEvent<T> capturedEvent;

    public void discard() {
        assertNotNull(capturedEvent, "There should be an event");
        capturedEvent = null;
    }

    public void assertEvent(boolean fromClient, T oldValue, T newValue) {
        assertNotNull(capturedEvent, "There should be an event");
        assertTrue(fromClient == capturedEvent.isFromClient());

        assertEventValues(capturedEvent, oldValue, newValue);

        discard();
    }

    public void assertNoEvent() {
        assertNull(capturedEvent, "There should be no event");
    }

    public static <T> void assertEventValues(ValueChangeEvent<T> event,
            T oldValue, T newValue) {
        assertEquals(oldValue, event.getOldValue());
        assertEquals(newValue, event.getValue());
    }
}
