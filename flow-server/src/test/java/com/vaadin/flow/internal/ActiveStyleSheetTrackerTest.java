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
package com.vaadin.flow.internal;

import java.util.Set;

import org.junit.Test;

import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ActiveStyleSheetTrackerTest {

    @Test
    public void singletonPerContext() {
        VaadinService service = new MockVaadinServletService();
        ActiveStyleSheetTracker t1 = ActiveStyleSheetTracker.get(service);
        ActiveStyleSheetTracker t2 = ActiveStyleSheetTracker.get(service);
        assertSame(t1, t2);
    }

    @Test
    public void addRemoveAndAppShellMerge() {
        VaadinService service = new MockVaadinServletService();

        ActiveStyleSheetTracker tracker = ActiveStyleSheetTracker.get(service);

        // Start with empty
        assertTrue(tracker.getActiveUrls().isEmpty());

        // Set AppShell URLs
        tracker.trackForAppShell(Set.of("context://css/app.css"));
        assertEquals(Set.of("context://css/app.css"), tracker.getActiveUrls());

        // Register component-based URL
        tracker.trackAddForComponent("context://css/view.css");

        // All active contains both (includes app shell + component)
        assertTrue(tracker.getActiveUrls().contains("context://css/app.css"));
        assertTrue(tracker.getActiveUrls().contains("context://css/view.css"));

        // Remove component URL
        tracker.trackRemoveForComponent("context://css/view.css");
        assertTrue(tracker.getActiveUrls().contains("context://css/app.css"));
        assertFalse(tracker.getActiveUrls().contains("context://css/view.css"));
    }
}
