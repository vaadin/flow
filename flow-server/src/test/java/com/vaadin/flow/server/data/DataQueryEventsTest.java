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
    void countStarted_exposesUiComponentAndFilter() {
        DataCountStartedEvent event = new DataCountStartedEvent(ui, component,
                true);

        assertSame(ui, event.getUI());
        assertSame(ui, event.getSource());
        assertSame(component, event.getComponent().orElseThrow());
        assertTrue(event.isFiltered());
    }

    @Test
    void countFailed_exposesTheThrowable() {
        IllegalStateException error = new IllegalStateException(
                "count is down");
        DataCountFailedEvent event = new DataCountFailedEvent(ui, component,
                false, error);

        assertSame(error, event.getError());
        assertFalse(event.isFiltered());
        assertSame(component, event.getComponent().orElseThrow());
    }

    @Test
    void countEnded_exposesTheCount() {
        assertEquals(42,
                new DataCountEndedEvent(ui, component, false, 42).getCount());
        assertEquals(-1,
                new DataCountEndedEvent(ui, component, false, -1).getCount(),
                "a count query that threw reports -1");
    }

    @Test
    void fetchStarted_exposesTheRequestedRange() {
        DataFetchStartedEvent event = new DataFetchStartedEvent(ui, component,
                20, 50, true);

        assertSame(ui, event.getUI());
        assertSame(component, event.getComponent().orElseThrow());
        assertEquals(20, event.getOffset());
        assertEquals(50, event.getLimit());
        assertTrue(event.isFiltered());
    }

    @Test
    void fetchFailed_exposesTheThrowable() {
        IllegalStateException error = new IllegalStateException("backend down");
        DataFetchFailedEvent event = new DataFetchFailedEvent(ui, component, 0,
                10, false, error);

        assertSame(error, event.getError());
        assertEquals(0, event.getOffset());
        assertEquals(10, event.getLimit());
    }

    @Test
    void fetchEnded_exposesRowsReturnedAgainstTheLimit() {
        DataFetchEndedEvent event = new DataFetchEndedEvent(ui, component, 0,
                50, false, 30);

        assertEquals(50, event.getLimit(), "what was asked for");
        assertEquals(30, event.getRowsReturned(), "what came back");
        assertEquals(-1,
                new DataFetchEndedEvent(ui, component, 0, 50, false, -1)
                        .getRowsReturned(),
                "a fetch that threw reports -1");
    }

    @Test
    void eventWithoutComponent_reportsEmptyComponent() {
        assertTrue(new DataFetchStartedEvent(ui, null, 0, 10, false)
                .getComponent().isEmpty());
        assertTrue(new DataCountStartedEvent(ui, null, false).getComponent()
                .isEmpty());
    }
}
