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

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.trigger.internal.DownloadAction;
import com.vaadin.flow.component.trigger.internal.OpenInNewTabAction;
import com.vaadin.flow.component.trigger.internal.TriggerInputJs;
import com.vaadin.flow.component.trigger.internal.TriggerTestUtil;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.snippetOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadTest {

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
    void startHandler_clickStartsDownloadOfRegisteredResource()
            throws Exception {
        Download.onClick(button, event -> event.inline("report.pdf"));

        JsFunction installFn = singleInstallFn();
        assertTrue(installFn.getCaptures().contains("click"),
                "Expected install captures to include the event name: "
                        + installFn.getCaptures());
        JsFunction action = actionOf(installFn);
        assertEquals("download",
                snippetOf(DownloadAction.DownloadJs.class, action));
        assertEquals("attachment; filename=\"report.pdf\"",
                servedContentDisposition(action),
                "A saved file should be served as an attachment");
    }

    @Test
    void startHandlerInNewTab_clickOpensRegisteredResourceInline()
            throws Exception {
        DownloadHandler handler = event -> event.setFileName("invoice.pdf");

        Download.onClick(button, handler, DownloadTarget.NEW_TAB);

        JsFunction action = actionOf(singleInstallFn());
        assertEquals("openInNewTab",
                snippetOf(OpenInNewTabAction.OpenInNewTabJs.class, action));
        assertEquals("inline; filename=\"invoice.pdf\"",
                servedContentDisposition(action));
    }

    @Test
    void startHandlerInNewTab_builtInHandler_handlerNotChanged()
            throws Exception {
        InputStreamDownloadHandler handler = DownloadHandler.fromInputStream(
                event -> DownloadResponse.error(500), "invoice.pdf");

        Download.onClick(button, handler, DownloadTarget.NEW_TAB);

        assertEquals("inline; filename=\"invoice.pdf\"",
                servedContentDisposition(actionOf(singleInstallFn())));
        assertEquals("attachment; filename=\"invoice.pdf\"",
                servedContentDisposition(handler, button.getElement()),
                "The handler should keep serving an attachment elsewhere");
    }

    @Test
    void startHandlerInNewTab_handlerAllowsDisabled_servedInline()
            throws Exception {
        DownloadHandler handler = event -> event.setFileName("invoice.pdf");

        Download.onClick(button, handler.allowDisabled(),
                DownloadTarget.NEW_TAB);

        JsFunction action = actionOf(singleInstallFn());
        assertEquals("inline; filename=\"invoice.pdf\"",
                servedContentDisposition(action));
        assertEquals(DisabledUpdateMode.ALWAYS,
                registeredResource(action).getElementRequestHandler()
                        .getDisabledUpdateMode(),
                "The content should still be served when the button is disabled");
    }

    @Test
    void startUrl_clickStartsDownloadOfGivenUrl() {
        Download.onClick(button, "/files/a.bin");

        JsFunction action = actionOf(singleInstallFn());
        assertEquals("download",
                snippetOf(DownloadAction.DownloadJs.class, action));
        assertEquals("/files/a.bin", ((JsFunction) action.getCaptures().get(0))
                .getCaptures().get(0));
    }

    @Test
    void startUrlInNewTab_clickOpensGivenUrl() {
        Download.onClick(button, "/files/terms.html", DownloadTarget.NEW_TAB);

        JsFunction action = actionOf(singleInstallFn());
        assertEquals("openInNewTab",
                snippetOf(OpenInNewTabAction.OpenInNewTabJs.class, action));
        assertEquals("/files/terms.html",
                ((JsFunction) action.getCaptures().get(0)).getCaptures()
                        .get(0));
    }

    @Test
    void startUrlWithFileName_clickStartsDownloadWithSuggestedName() {
        Download.onClick(button, "/files/a.bin", "b.bin");

        JsFunction action = actionOf(singleInstallFn());
        assertEquals("download",
                snippetOf(DownloadAction.DownloadJs.class, action));
        assertEquals("returnValue", snippetOf(TriggerInputJs.class,
                ((JsFunction) action.getCaptures().get(1))));
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

    @Test
    void removeNewTabRegistration_handlerNoLongerServed() {
        Registration registration = Download.onClick(button,
                event -> event.getOutputStream().write(1),
                DownloadTarget.NEW_TAB);
        URI uri = URI.create(resourceUri(actionOf(singleInstallFn())));
        assertTrue(registry.getResource(uri).isPresent());

        registration.remove();

        assertFalse(registry.getResource(uri).isPresent(),
                "Removing the registration should unregister the handler");
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

    private String servedContentDisposition(JsFunction action)
            throws Exception {
        ElementStreamResource resource = registeredResource(action);
        return servedContentDisposition(
                (DownloadHandler) resource.getElementRequestHandler(),
                resource.getOwner());
    }

    private ElementStreamResource registeredResource(JsFunction action) {
        URI uri = URI.create(resourceUri(action));
        return (ElementStreamResource) registry.getResource(uri).orElseThrow();
    }

    private static String servedContentDisposition(DownloadHandler handler,
            Element owner) throws Exception {
        VaadinResponse response = mock(VaadinResponse.class);
        AtomicReference<String> header = new AtomicReference<>();
        doAnswer(invocation -> {
            header.set(invocation.getArgument(1));
            return null;
        }).when(response).setHeader(eq("Content-Disposition"), any());
        handler.handleRequest(mock(VaadinRequest.class), response,
                mock(VaadinSession.class), owner);
        return header.get();
    }

    private JsFunction singleInstallFn() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return TriggerTestUtil.installFn(pending.get(0).getInvocation());
    }

    private static JsFunction actionOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }
}
