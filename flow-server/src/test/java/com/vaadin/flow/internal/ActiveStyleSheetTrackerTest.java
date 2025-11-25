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
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.Assert.assertEquals;
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
        VaadinSession session = new MockVaadinSession(service);

        ActiveStyleSheetTracker tracker = ActiveStyleSheetTracker.get(service);

        // Start with empty
        assertTrue(tracker.getActiveUrls().isEmpty());
        assertTrue(tracker.getActiveUrls(session).isEmpty());

        // Set AppShell URLs
        tracker.setAppShellUrls(Set.of("context://css/app.css"));
        assertEquals(Set.of("context://css/app.css"), tracker.getActiveUrls());
        assertEquals(Set.of("context://css/app.css"),
                tracker.getActiveUrls(session));

        // Register session-specific URL
        tracker.registerAdded(session, "context://css/view.css");
        assertEquals(Set.of("context://css/app.css", "context://css/view.css"),
                tracker.getActiveUrls(session));
        // All active contains both (includes app shell + session)
        assertTrue(tracker.getActiveUrls().contains("context://css/app.css"));
        assertTrue(tracker.getActiveUrls().contains("context://css/view.css"));

        // Remove session URL
        tracker.registerRemoved(session, "context://css/view.css");
        assertEquals(Set.of("context://css/app.css"),
                tracker.getActiveUrls(session));
    }
}
