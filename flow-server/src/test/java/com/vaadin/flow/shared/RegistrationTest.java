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
package com.vaadin.flow.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.Command;

public class RegistrationTest {
    @Test
    public void once_onlyCalledOnce() {
        AtomicBoolean invoked = new AtomicBoolean();
        Command action = () -> {
            boolean calledPreviously = invoked.getAndSet(true);

            Assert.assertFalse("Command should not invoked previously",
                    calledPreviously);
        };

        Registration registration = Registration.once(action);

        Assert.assertFalse("Command should not yet be invoked", invoked.get());

        registration.remove();

        Assert.assertTrue("Command should be invoked", invoked.get());

        // Action will throw if invoked again
        registration.remove();
    }

    @Test
    public void combine_removesAll() {
        AtomicBoolean firstRemoved = new AtomicBoolean();
        AtomicBoolean secondRemoved = new AtomicBoolean();

        Registration registration = Registration.combine(
                () -> firstRemoved.set(true), () -> secondRemoved.set(true));

        Assert.assertFalse("Should not be removed yet", firstRemoved.get());
        Assert.assertFalse("Should not be removed yet", secondRemoved.get());

        registration.remove();

        Assert.assertTrue("Should be removed now", firstRemoved.get());
        Assert.assertTrue("Should be removed now", secondRemoved.get());
    }

    @Test
    public void addAndRemove_addsAndRemoves() {
        Collection<Object> collection = new ArrayList<>();
        Object o1 = new Object();
        Object o2 = new Object();

        Registration r1 = Registration.addAndRemove(collection, o1);
        Assert.assertEquals(1, collection.size());
        Assert.assertTrue(collection.contains(o1));

        Registration r2 = Registration.addAndRemove(collection, o2);
        Assert.assertEquals(2, collection.size());
        Assert.assertTrue(collection.contains(o2));

        r1.remove();
        Assert.assertEquals(1, collection.size());
        Assert.assertFalse(collection.contains(o1));

        r2.remove();
        Assert.assertTrue(collection.isEmpty());
    }
}
