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
package com.vaadin.flow.server.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers what the data query events expose. Dispatch itself belongs to
 * {@code VaadinServiceEventBusTest}, and the communicator side to
 * {@code DataFetchEventsTest} in {@code flow-data}.
 */
class DataQueryEventsTest {

    @Tag("test-list")
    private static class TestComponent extends Component {
    }

    private UI ui;
    private TestComponent component;

    @BeforeEach
    void init() {
        VaadinSession session = new AlwaysLockedVaadinSession(
                new MockVaadinServletService());
        VaadinSession.setCurrent(session);
        ui = new MockUI(session);
        component = new TestComponent();
        ui.add(component);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    @Test
    void endedEvents_reportMinusOneWhenTheQueryThrew() {
        assertEquals(42,
                new DataCountEndedEvent(ui, component, false, 42).getCount());
        assertEquals(-1,
                new DataCountEndedEvent(ui, component, false, -1).getCount(),
                "a count query that threw reports -1, not a count of zero");

        assertEquals(30,
                new DataFetchEndedEvent(ui, component, 0, 50, false, 30)
                        .getRowsReturned());
        assertEquals(-1,
                new DataFetchEndedEvent(ui, component, 0, 50, false, -1)
                        .getRowsReturned(),
                "a fetch that threw reports -1, not an empty page");
    }

    @Test
    void fetchEvents_keepRequestedAndReturnedApart() {
        DataFetchStartedEvent started = new DataFetchStartedEvent(ui, component,
                20, 50, true);
        assertEquals(20, started.getOffset());
        assertEquals(50, started.getLimit());
        assertTrue(started.isFiltered(),
                "a combo box loading matches for typed text is a filtered "
                        + "query");

        // A short page is the signal that a data provider returned less than
        // the component asked for, so the two numbers must stay separate.
        DataFetchEndedEvent ended = new DataFetchEndedEvent(ui, component, 20,
                50, true, 30);
        assertEquals(50, ended.getLimit());
        assertEquals(30, ended.getRowsReturned());

        IllegalStateException error = new IllegalStateException("backend down");
        assertSame(error,
                new DataFetchFailedEvent(ui, component, 20, 50, true, error)
                        .getError());
    }

    @Test
    void componentIsOptional_becauseANodeNeedNotBeAComponent() {
        // A data communicator driven by a bare element has no component, so
        // observers must be able to attribute a query to nothing.
        assertTrue(new DataFetchStartedEvent(ui, null, 0, 10, false)
                .getComponent().isEmpty());
        assertTrue(new DataCountStartedEvent(ui, null, false).getComponent()
                .isEmpty());

        IllegalStateException error = new IllegalStateException(
                "count is down");
        DataCountFailedEvent attributed = new DataCountFailedEvent(ui,
                component, false, error);
        assertSame(error, attributed.getError(),
                "a failed count carries its throwable, as a failed fetch does");
        assertSame(component, attributed.getComponent().orElseThrow());
        assertSame(ui, attributed.getUI());
        assertSame(ui, attributed.getSource(),
                "the UI is the event source, as for the RPC events");
        assertFalse(attributed.isFiltered());
    }
}
