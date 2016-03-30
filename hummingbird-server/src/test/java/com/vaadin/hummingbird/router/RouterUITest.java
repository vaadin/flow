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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.History;
import com.vaadin.ui.History.HistoryStateChangeEvent;
import com.vaadin.ui.UI;

public class RouterUITest {
    private static class TestUI extends UI {
        // Custom router so we don't have to set up a VaadinService for the test
        private Router router = new Router();

        public TestUI(String initialLocation) {
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            String pathInfo;
            if (initialLocation.isEmpty()) {
                pathInfo = null;
            } else {
                Assert.assertFalse(initialLocation.startsWith("/"));
                pathInfo = "/" + initialLocation;
            }
            Mockito.when(request.getPathInfo()).thenReturn(pathInfo);

            router.reconfigure(c -> {
            });

            router.initializeUI(this, request);
        }

        @Override
        protected Router getRouter() {
            return router;
        }
    }

    @Test
    public void testInitialLocation() {
        TestUI ui = new TestUI("foo/bar");

        Assert.assertEquals("foo/bar", ui.getActiveViewLocation().getPath());
    }

    @Test
    public void locationAfterServerNavigation() {
        TestUI ui = new TestUI("");

        ui.navigateTo("foo/bar");

        Assert.assertEquals("foo/bar", ui.getActiveViewLocation().getPath());
    }

    @Test
    public void locationAfterClientNavigation() {
        TestUI ui = new TestUI("");

        History history = ui.getPage().getHistory();

        history.getHistoryStateChangeHandler().onHistoryStateChange(
                new HistoryStateChangeEvent(history, null, "foo/bar"));

        Assert.assertEquals("foo/bar", ui.getActiveViewLocation().getPath());
    }

    @Test
    public void testInvalidNavigationTargets() {
        String[] invalidTargets = { null, "/foo", "http://vaadin.com",
                "://vaadin.com", "foo/bar/..", "foo/ba%r" };
        for (String invalidTarget : invalidTargets) {
            TestUI ui = new TestUI("");
            try {
                ui.navigateTo(invalidTarget);
                Assert.fail("Navigation target should cause exception: "
                        + invalidTarget);
            } catch (IllegalArgumentException expected) {
                // All is fine
            }
        }
    }
}
