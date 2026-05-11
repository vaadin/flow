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
package com.vaadin.flow.component.page;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullscreenSessionTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    private MockUI ui;
    private TestPage page;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        page = new TestPage(ui);
        ui.setPage(page);
    }

    @Test
    void requestFullscreen_startsInPending() {
        FullscreenSession session = page.requestFullscreen();
        assertEquals(FullscreenSessionState.PENDING,
                session.stateSignal().peek());
        assertTrue(session.owner().isEmpty(),
                "Page-level session has no owner");
    }

    @Test
    void requestFullscreen_acceptedOutcome_transitionsToActive() {
        FullscreenSession session = page.requestFullscreen();
        page.lastResult.fireSuccess(outcome(true, null));
        assertEquals(FullscreenSessionState.ACTIVE,
                session.stateSignal().peek());
    }

    @Test
    void requestFullscreen_rejectedOutcome_transitionsToRejectedAndCarriesError() {
        FullscreenSession session = page.requestFullscreen();
        page.lastResult.fireSuccess(outcome(false, "no user activation"));
        assertEquals(FullscreenSessionState.REJECTED,
                session.stateSignal().peek());
        assertEquals("no user activation", session.error().orElse(null));
    }

    @Test
    void requestFullscreen_clientError_transitionsToRejected() {
        FullscreenSession session = page.requestFullscreen();
        page.lastResult.fireError("script crash");
        assertEquals(FullscreenSessionState.REJECTED,
                session.stateSignal().peek());
        assertEquals("script crash", session.error().orElse(null));
    }

    @Test
    void requestFullscreen_userExit_transitionsToExitedByUser() {
        FullscreenSession session = page.requestFullscreen();
        page.lastResult.fireSuccess(outcome(true, null));
        page.simulateFullscreenChange(FullscreenState.FULLSCREEN);
        page.simulateFullscreenChange(FullscreenState.NOT_FULLSCREEN);
        assertEquals(FullscreenSessionState.EXITED_BY_USER,
                session.stateSignal().peek());
    }

    @Test
    void exitFullscreen_transitionsActiveSessionToExitedByCode() {
        FullscreenSession session = page.requestFullscreen();
        page.lastResult.fireSuccess(outcome(true, null));
        page.simulateFullscreenChange(FullscreenState.FULLSCREEN);

        page.exitFullscreen();
        page.simulateFullscreenChange(FullscreenState.NOT_FULLSCREEN);

        assertEquals(FullscreenSessionState.EXITED_BY_CODE,
                session.stateSignal().peek());
    }

    @Test
    void newRequest_supersedesActiveSession() {
        FullscreenSession first = page.requestFullscreen();
        page.lastResult.fireSuccess(outcome(true, null));

        FullscreenSession second = page.requestFullscreen();

        assertEquals(FullscreenSessionState.EXITED_BY_CODE,
                first.stateSignal().peek(),
                "Superseded session should end in EXITED_BY_CODE");
        assertEquals(FullscreenSessionState.PENDING,
                second.stateSignal().peek());
        assertNotSame(first, second);
    }

    @Test
    void requestFullscreenForComponent_carriesOwner() {
        TestComponent component = new TestComponent();
        ui.add(component);

        FullscreenSession session = page.requestFullscreen(component);

        assertSame(component, session.owner().orElseThrow());
    }

    @Test
    void requestFullscreenForComponent_nullThrows() {
        assertThrows(NullPointerException.class,
                () -> page.requestFullscreen(null));
    }

    @Test
    void componentRequestFullscreen_returnsSession() {
        TestComponent component = new TestComponent();
        ui.add(component);

        FullscreenSession session = component.requestFullscreen();

        assertEquals(FullscreenSessionState.PENDING,
                session.stateSignal().peek());
        assertSame(component, session.owner().orElseThrow());
    }

    @Test
    void componentExitFullscreen_callsPageExitFullscreen() {
        TestComponent component = new TestComponent();
        ui.add(component);
        page.requestFullscreen(component);
        page.lastResult.fireSuccess(outcome(true, null));
        page.simulateFullscreenChange(FullscreenState.FULLSCREEN);

        component.exitFullscreen();
        page.simulateFullscreenChange(FullscreenState.NOT_FULLSCREEN);

        FullscreenSession session = (FullscreenSession) page.lastSession;
        assertEquals(FullscreenSessionState.EXITED_BY_CODE,
                session.stateSignal().peek());
    }

    private static JsonNode outcome(boolean ok, String error) {
        tools.jackson.databind.node.ObjectNode node = JacksonUtils
                .createObjectNode();
        node.put("ok", ok);
        if (error != null) {
            node.put("error", error);
        }
        return node;
    }

    /**
     * Captures every executeJs invocation and lets the test fire the success or
     * error callback synchronously, replacing the asynchronous client
     * round-trip. Also records the most recent session returned from a
     * fullscreen request so tests can assert on it after delegating through
     * Component or Page-level overloads.
     */
    private static class TestPage extends Page {
        CapturingPendingResult lastResult;
        FullscreenSession lastSession;

        TestPage(MockUI ui) {
            super(ui);
        }

        @Override
        public PendingJavaScriptResult executeJs(String expression,
                Object... parameters) {
            CapturingPendingResult result = new CapturingPendingResult();
            lastResult = result;
            return result;
        }

        @Override
        public FullscreenSession requestFullscreen() {
            FullscreenSession session = super.requestFullscreen();
            lastSession = session;
            return session;
        }

        @Override
        public FullscreenSession requestFullscreen(Component component) {
            FullscreenSession session = super.requestFullscreen(component);
            lastSession = session;
            return session;
        }
    }

    private static class CapturingPendingResult
            implements PendingJavaScriptResult {
        private final List<SerializableConsumer<JsonNode>> successHandlers = new ArrayList<>();
        private final List<SerializableConsumer<String>> errorHandlers = new ArrayList<>();

        @Override
        public boolean cancelExecution() {
            return false;
        }

        @Override
        public boolean isSentToBrowser() {
            return false;
        }

        @Override
        public void then(SerializableConsumer<JsonNode> resultHandler,
                SerializableConsumer<String> errorHandler) {
            if (resultHandler != null) {
                successHandlers.add(resultHandler);
            }
            if (errorHandler != null) {
                errorHandlers.add(errorHandler);
            }
        }

        void fireSuccess(JsonNode json) {
            successHandlers.forEach(h -> h.accept(json));
        }

        void fireError(String error) {
            errorHandlers.forEach(h -> h.accept(error));
        }
    }
}
