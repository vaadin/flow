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
package com.vaadin.hummingbird.router;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class RouterTest {

    @Test
    public void testResolve() {
        RouterUI ui = new RouterUI();

        AtomicReference<Location> resolvedLocation = new AtomicReference<>();
        AtomicReference<NavigationEvent> handledEvent = new AtomicReference();

        Router router = new Router();
        router.setResolver(eventToResolve -> {
            Assert.assertNull(resolvedLocation.get());
            resolvedLocation.set(eventToResolve.getLocation());
            return new NavigationHandler() {
                @Override
                public void handle(NavigationEvent eventToHandle) {
                    Assert.assertNull(handledEvent.get());
                    handledEvent.set(eventToHandle);
                }
            };
        });

        Assert.assertNull(resolvedLocation.get());
        Assert.assertNull(handledEvent.get());

        Location testLocation = new Location("");

        router.navigate(ui, testLocation);

        Assert.assertSame(testLocation, resolvedLocation.get());
        Assert.assertSame(testLocation, handledEvent.get().getLocation());
        Assert.assertSame(ui, handledEvent.get().getUI());
    }

    @Test
    public void testResolveError() {
        RouterUI ui = new RouterUI();

        Router router = new Router();
        router.setResolver(event -> null);

        router.navigate(ui, new Location(""));

        Assert.assertTrue(ui.getElement().getTextContent().contains("404"));
    }
}
