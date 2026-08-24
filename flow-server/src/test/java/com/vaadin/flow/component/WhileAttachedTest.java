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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WhileAttachedTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    private final List<String> log = new ArrayList<>();

    private Registration logging(TestComponent component) {
        return logging(component, "");
    }

    private Registration logging(TestComponent component, String prefix) {
        return component.whileAttached(ui -> {
            log.add(prefix + "attach");
            return () -> log.add(prefix + "detach");
        });
    }

    @Test
    void notAttached_handlerNotRunUntilAttach() {
        TestComponent component = new TestComponent();
        logging(component);
        assertEquals(List.of(), log);

        new MockUI().add(component);
        assertEquals(List.of("attach"), log);
    }

    @Test
    void alreadyAttached_handlerRunImmediately() {
        TestComponent component = new TestComponent();
        new MockUI().add(component);

        logging(component);
        assertEquals(List.of("attach"), log);
    }

    @Test
    void detach_cleanupRun() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);
        logging(component);

        ui.remove(component);
        assertEquals(List.of("attach", "detach"), log);
    }

    @Test
    void reattach_handlerRunAgain() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        logging(component);

        ui.add(component);
        ui.remove(component);
        ui.add(component);

        assertEquals(List.of("attach", "detach", "attach"), log);
    }

    @Test
    void handlerReceivesCurrentUi() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);

        List<UI> seen = new ArrayList<>();
        component.whileAttached(handlerUi -> {
            seen.add(handlerUi);
            return null;
        });

        assertEquals(1, seen.size());
        assertSame(ui, seen.get(0));
    }

    @Test
    void nullCleanupAccepted() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);
        component.whileAttached(ignored -> null);

        ui.remove(component);
        ui.add(component);
    }

    @Test
    void removeRegistrationWhileAttached_cleanupRunAndHandlerNotRunAgain() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);
        Registration registration = logging(component);

        registration.remove();
        assertEquals(List.of("attach", "detach"), log);

        ui.remove(component);
        ui.add(component);
        assertEquals(List.of("attach", "detach"), log);
    }

    @Test
    void removeRegistrationWhileDetached_noCleanupAndHandlerNotRunAgain() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);
        Registration registration = logging(component);
        ui.remove(component);

        registration.remove();
        assertEquals(List.of("attach", "detach"), log);

        ui.add(component);
        assertEquals(List.of("attach", "detach"), log);
    }

    @Test
    void removeRegistrationTwice_cleanupRunOnce() {
        TestComponent component = new TestComponent();
        new MockUI().add(component);
        Registration registration = logging(component);

        registration.remove();
        registration.remove();
        assertEquals(List.of("attach", "detach"), log);
    }

    @Test
    void multipleScopes_independent() {
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);
        Registration first = logging(component, "first-");
        logging(component, "second-");

        first.remove();
        ui.remove(component);

        assertEquals(List.of("first-attach", "second-attach", "first-detach",
                "second-detach"), log);
    }

    @Test
    void moveToNewUiWithoutDetachEvent_cleanupRunBeforeNewAttach() {
        // Mimics UIInternals.moveToNewUI for @PreserveOnRefresh, which
        // re-attaches the node without firing a detach event
        TestComponent component = new TestComponent();
        MockUI ui = new MockUI();
        ui.add(component);
        logging(component);

        component.getElement().getNode().removeFromTree(false);
        new MockUI().add(component);

        assertEquals(List.of("attach", "detach", "attach"), log);
    }

    @Test
    void elementLevelApi_handlerRunOnAttachAndDetach() {
        Element element = new Element("div");
        MockUI ui = new MockUI();
        element.whileAttached(handlerUi -> {
            log.add("attach");
            return () -> log.add("detach");
        });

        ui.getElement().appendChild(element);
        element.removeFromParent();

        assertEquals(List.of("attach", "detach"), log);
    }

    @Test
    void failingCleanup_reportedToErrorHandlerAndOtherCleanupsRun() {
        CurrentInstance.clearAll();
        MockVaadinServletService service = new MockVaadinServletService();
        VaadinService.setCurrent(service);
        MockVaadinSession session = new MockVaadinSession(service);
        session.lock();
        List<ErrorEvent> errors = new ArrayList<>();
        session.setErrorHandler(errors::add);

        TestComponent component = new TestComponent();
        MockUI ui = new MockUI(session);
        ui.add(component);

        component.whileAttached(ignored -> () -> {
            throw new IllegalStateException("cleanup failed");
        });
        logging(component);

        ui.remove(component);

        assertEquals(List.of("attach", "detach"), log);
        assertEquals(1, errors.size());
        assertEquals("cleanup failed",
                errors.get(0).getThrowable().getMessage());
    }

    @Test
    void nullHandler_throws() {
        TestComponent component = new TestComponent();
        assertThrows(NullPointerException.class,
                () -> component.whileAttached(null));
    }
}
