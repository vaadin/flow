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
package com.vaadin.flow.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationTest {
    @Test
    public void once_onlyCalledOnce() {
        AtomicBoolean invoked = new AtomicBoolean();
        Command action = () -> {
            boolean calledPreviously = invoked.getAndSet(true);

            assertFalse(calledPreviously,
                    "Command should not invoked previously");
        };

        Registration registration = Registration.once(action);

        assertFalse(invoked.get(), "Command should not yet be invoked");

        registration.remove();

        assertTrue(invoked.get(), "Command should be invoked");

        // Action will throw if invoked again
        registration.remove();
    }

    @Test
    public void combine_removesAll() {
        AtomicBoolean firstRemoved = new AtomicBoolean();
        AtomicBoolean secondRemoved = new AtomicBoolean();

        Registration registration = Registration.combine(
                () -> firstRemoved.set(true), () -> secondRemoved.set(true));

        assertFalse(firstRemoved.get(), "Should not be removed yet");
        assertFalse(secondRemoved.get(), "Should not be removed yet");

        registration.remove();

        assertTrue(firstRemoved.get(), "Should be removed now");
        assertTrue(secondRemoved.get(), "Should be removed now");
    }

    @Test
    public void addAndRemove_addsAndRemoves() {
        Collection<Object> collection = new ArrayList<>();
        Object o1 = new Object();
        Object o2 = new Object();

        Registration r1 = Registration.addAndRemove(collection, o1);
        assertEquals(1, collection.size());
        assertTrue(collection.contains(o1));

        Registration r2 = Registration.addAndRemove(collection, o2);
        assertEquals(2, collection.size());
        assertTrue(collection.contains(o2));

        r1.remove();
        assertEquals(1, collection.size());
        assertFalse(collection.contains(o1));

        r2.remove();
        assertTrue(collection.isEmpty());
    }
}
