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
package com.vaadin.flow.component.download;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DownloadTest {

    @Tag("test-button")
    static final class TestButton extends Component
            implements ClickNotifier<TestButton> {
    }

    private UI ui;
    private TestButton button;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        // The mock session has no resource registry by default; install a
        // real one so a DownloadHandler can be registered.
        VaadinSession session = ui.getSessionOrThrow();
        when(session.getResourceRegistry())
                .thenReturn(new StreamResourceRegistry(session));
        button = new TestButton();
        ui.getElement().appendChild(button.getElement());
    }

    @Test
    void startHandler_clickStartsDownloadOfRegisteredResource() {
        Download.onClick(button, event -> event.getOutputStream().write(1));

        JsFunction installFn = singleInstallFn();
        assertTrue(installFn.getCaptures().contains("click"),
                "Expected install captures to include the event name: "
                        + installFn.getCaptures());
        JsFunction action = actionOf(installFn);
        assertEquals("window.Vaadin.Flow.download.start($0(event))",
                action.getBody());
        resourceUri(action);
    }

    @Test
    void startUrlWithFileName_clickStartsDownloadWithSuggestedName() {
        Download.onClick(button, "/files/a.bin", "b.bin");

        JsFunction action = actionOf(singleInstallFn());
        assertEquals("window.Vaadin.Flow.download.start($0(event), $1(event))",
                action.getBody());
        assertEquals("return $0",
                ((JsFunction) action.getCaptures().get(1)).getBody());
        assertEquals("b.bin", ((JsFunction) action.getCaptures().get(1))
                .getCaptures().get(0));
    }

    @Test
    void removeRegistration_disposesClickListener() {
        Registration registration = Download.onClick(button, "/files/a.bin");
        singleInstallFn();
        ui.getInternals().getStateTree().collectChanges(c -> {
        });

        registration.remove();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size());
        assertTrue(
                pending.get(0).getInvocation().getExpression()
                        .contains("disposeInitializer"),
                "Removal should emit the dispose invocation");
    }

    private static String resourceUri(JsFunction action) {
        Object uri = ((JsFunction) action.getCaptures().get(0)).getCaptures()
                .get(0);
        assertTrue(
                uri instanceof String s
                        && s.startsWith("VAADIN/dynamic/resource/"),
                "Expected a Vaadin dynamic-resource URI, got: " + uri);
        return (String) uri;
    }

    private JsFunction singleInstallFn() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        Object o = pending.get(0).getInvocation().getParameters().get(2);
        assertTrue(o instanceof JsFunction,
                "Expected install param $2 to be a JsFunction");
        return (JsFunction) o;
    }

    private static JsFunction actionOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }
}
