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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.Command;

class RegistrationTest {
    @Test
    public void once_onlyCalledOnce() {
        AtomicBoolean invoked = new AtomicBoolean();
        Command action = () -> {
            boolean calledPreviously = invoked.getAndSet(true);

            Assertions.assertFalse(calledPreviously,
                    "Command should not invoked previously");
        };

        Registration registration = Registration.once(action);

        Assertions.assertFalse(invoked.get(),
                "Command should not yet be invoked");

        registration.remove();

        Assertions.assertTrue(invoked.get(), "Command should be invoked");

        // Action will throw if invoked again
        registration.remove();
    }

    @Test
    public void combine_removesAll() {
        AtomicBoolean firstRemoved = new AtomicBoolean();
        AtomicBoolean secondRemoved = new AtomicBoolean();

        Registration registration = Registration.combine(
                () -> firstRemoved.set(true), () -> secondRemoved.set(true));

        Assertions.assertFalse(firstRemoved.get(), "Should not be removed yet");
        Assertions.assertFalse(secondRemoved.get(),
                "Should not be removed yet");

        registration.remove();

        Assertions.assertTrue(firstRemoved.get(), "Should be removed now");
        Assertions.assertTrue(secondRemoved.get(), "Should be removed now");
    }

    @Test
    public void addAndRemove_addsAndRemoves() {
        Collection<Object> collection = new ArrayList<>();
        Object o1 = new Object();
        Object o2 = new Object();

        Registration r1 = Registration.addAndRemove(collection, o1);
        Assertions.assertEquals(1, collection.size());
        Assertions.assertTrue(collection.contains(o1));

        Registration r2 = Registration.addAndRemove(collection, o2);
        Assertions.assertEquals(2, collection.size());
        Assertions.assertTrue(collection.contains(o2));

        r1.remove();
        Assertions.assertEquals(1, collection.size());
        Assertions.assertFalse(collection.contains(o1));

        r2.remove();
        Assertions.assertTrue(collection.isEmpty());
    }
}
