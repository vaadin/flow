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
package com.vaadin.flow.component.trigger.internal;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.installFns;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DownloadActionTest {

    @Test
    void urlString_actionCallsStartWithOneArgument() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new DownloadAction("/api/report.pdf"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("window.Vaadin.Flow.download.start($0(event))",
                action.getBody());
        assertLiteralInputValue(action, 0, "/api/report.pdf");
    }

    @Test
    void urlStringWithFilename_actionCallsStartWithTwoArguments() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(
                new DownloadAction("/api/report.pdf", "Q1 \"report\".pdf"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Two-argument body shape; filename value (with embedded quote) goes
        // verbatim into the input function's capture — Jackson handles the
        // escaping on the wire, no hand-quoting in the JS.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("window.Vaadin.Flow.download.start($0(event), $1(event))",
                action.getBody());
        assertLiteralInputValue(action, 0, "/api/report.pdf");
        assertLiteralInputValue(action, 1, "Q1 \"report\".pdf");
    }

    @Test
    void inputUrl_capturesPropertyInputThatReadsAtFireTime() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click").triggers(new DownloadAction(
                new PropertyInput<>(field, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        JsFunction urlFn = (JsFunction) action.getCaptures().get(0);
        assertEquals("return $0[$1]", urlFn.getBody());
        assertEquals("value", urlFn.getCaptures().get(1));
    }

    @Test
    void downloadHandler_capturesRegisteredResourceUri() {
        UI ui = new MockUI();
        // The mock session has no resource registry by default; install a
        // real one so the action can register its DownloadHandler.
        VaadinSession session = requireNonNull(ui.getSession());
        when(session.getResourceRegistry())
                .thenReturn(new StreamResourceRegistry(session));
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        DownloadHandler handler = event -> event.getOutputStream()
                .write("hi".getBytes());
        new DomEventTrigger(button, "click")
                .triggers(new DownloadAction(handler));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // The URI itself contains a UUID we don't control, so just assert
        // the structural shape: the URL input captures the registered
        // Vaadin dynamic-resource path as a plain String.
        JsFunction action = actionOf(singleInstallFn(ui));
        JsFunction urlFn = (JsFunction) action.getCaptures().get(0);
        Object uri = urlFn.getCaptures().get(0);
        assertTrue(
                uri instanceof String && ((String) uri)
                        .startsWith("VAADIN/dynamic/resource/"),
                "Expected a Vaadin dynamic-resource URI, got: " + uri);
    }

    @Test
    void downloadHandler_triggerRemoved_unregistersResource() {
        UI ui = new MockUI();
        VaadinSession session = requireNonNull(ui.getSession());
        StreamResourceRegistry registry = new StreamResourceRegistry(session);
        when(session.getResourceRegistry()).thenReturn(registry);
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        trigger.triggers(new DownloadAction(
                (DownloadHandler) event -> event.getOutputStream().write(1)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        URI uri = URI.create((String) ((JsFunction) action.getCaptures().get(0))
                .getCaptures().get(0));
        assertTrue(registry.getResource(uri).isPresent());

        trigger.remove();

        assertFalse(registry.getResource(uri).isPresent(),
                "Removing the trigger should unregister the handler");
    }

    @Test
    void downloadHandler_sharedOnSameHost_removingOneTriggerKeepsOther() {
        UI ui = new MockUI();
        VaadinSession session = requireNonNull(ui.getSession());
        StreamResourceRegistry registry = new StreamResourceRegistry(session);
        when(session.getResourceRegistry()).thenReturn(registry);
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        DownloadAction download = new DownloadAction(
                (DownloadHandler) event -> event.getOutputStream().write(1));
        DomEventTrigger click = new DomEventTrigger(button, "click");
        click.triggers(download);
        DomEventTrigger keydown = new DomEventTrigger(button, "keydown");
        keydown.triggers(download);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<URI> uris = installFns(ui).stream().map(fn -> URI.create(
                (String) ((JsFunction) actionOf(fn).getCaptures().get(0))
                        .getCaptures().get(0)))
                .toList();
        assertEquals(2, uris.size());

        click.remove();

        assertFalse(registry.getResource(uris.get(0)).isPresent(),
                "Removing the trigger should unregister its handler");
        assertTrue(registry.getResource(uris.get(1)).isPresent(),
                "The other trigger's URL should keep serving the file");
    }

    private static void assertLiteralInputValue(JsFunction action,
            int captureIndex, String expectedValue) {
        Object capture = action.getCaptures().get(captureIndex);
        assertTrue(capture instanceof JsFunction,
                "expected JsFunction at capture " + captureIndex);
        JsFunction inputFn = (JsFunction) capture;
        assertEquals("return $0", inputFn.getBody(),
                "expected LiteralInput shape");
        assertEquals(expectedValue, inputFn.getCaptures().get(0));
    }
}
