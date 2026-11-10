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
package com.vaadin.flow.component.newtab;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.FileDownloadHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class OpenInNewTabTest {

    @Tag("test-button")
    static final class TestButton extends Component {
    }

    private UI ui;
    private TestButton button;
    private StreamResourceRegistry registry;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        // The mock session has no resource registry by default; install a
        // real one so a DownloadHandler can be registered.
        VaadinSession session = ui.getSessionOrThrow();
        registry = new StreamResourceRegistry(session);
        when(session.getResourceRegistry()).thenReturn(registry);
        button = new TestButton();
        ui.getElement().appendChild(button.getElement());
    }

    @Test
    void openHandler_clickOpensRegisteredResourceInline() {
        FileDownloadHandler handler = DownloadHandler
                .forFile(new File("invoice.pdf"));

        OpenInNewTab.onClick(button, handler);

        assertTrue(handler.isInline(),
                "A built-in handler should serve its content inline");
        JsFunction action = actionOf(singleInstallFn());
        assertTrue(action.getBody().contains("window.open(u, \"_blank\""),
                "Expected a window.open call, got: " + action.getBody());
        Object uri = urlCapture(action);
        assertTrue(
                uri instanceof String s
                        && s.startsWith("VAADIN/dynamic/resource/"),
                "Expected a Vaadin dynamic-resource URI, got: " + uri);
    }

    @Test
    void openUrl_clickOpensGivenUrl() {
        OpenInNewTab.onClick(button, "/files/terms.html");

        JsFunction action = actionOf(singleInstallFn());
        assertTrue(action.getBody().contains("window.open(u, \"_blank\""),
                "Expected a window.open call, got: " + action.getBody());
        assertEquals("/files/terms.html", urlCapture(action));
    }

    @Test
    void removeRegistration_handlerNoLongerServed() {
        Registration registration = OpenInNewTab.onClick(button,
                event -> event.getOutputStream().write(1));
        URI uri = URI.create((String) urlCapture(actionOf(singleInstallFn())));
        assertTrue(registry.getResource(uri).isPresent());

        registration.remove();

        assertFalse(registry.getResource(uri).isPresent(),
                "Removing the registration should unregister the handler");
    }

    private static Object urlCapture(JsFunction action) {
        return ((JsFunction) action.getCaptures().get(0)).getCaptures().get(0);
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
