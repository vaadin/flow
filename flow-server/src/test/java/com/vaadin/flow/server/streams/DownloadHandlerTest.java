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
package com.vaadin.flow.server.streams;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class DownloadHandlerTest {

    @Test
    void allowDisabled_lambda_disabledUpdateModeIsAlways() {
        DownloadHandler delegate = event -> {
        };

        DownloadHandler wrapped = delegate.allowDisabled();

        assertEquals(DisabledUpdateMode.ALWAYS,
                wrapped.getDisabledUpdateMode());
    }

    @Test
    void allowDisabled_lambda_forwardsHandleDownloadRequest() throws Exception {
        AtomicReference<DownloadEvent> received = new AtomicReference<>();
        DownloadHandler delegate = received::set;

        DownloadEvent event = new DownloadEvent(mock(VaadinRequest.class),
                mock(VaadinResponse.class), mock(VaadinSession.class),
                mock(Element.class));

        delegate.allowDisabled().handleDownloadRequest(event);

        assertSame(event, received.get(),
                "Wrapped handler must forward the event to the delegate");
    }

    @ParameterizedTest
    @ValueSource(strings = { "allowDisabled", "serveInline",
            "serveAsAttachment" })
    void views_forwardUrlPostfixAllowInertAndRangeRequests(String view) {
        DownloadHandler delegate = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }

            @Override
            public String getUrlPostfix() {
                return "icon.svg";
            }

            @Override
            public boolean isAllowInert() {
                return true;
            }

            @Override
            public boolean isRangeRequestsEnabled() {
                return true;
            }
        };

        DownloadHandler wrapped = switch (view) {
        case "allowDisabled" -> delegate.allowDisabled();
        case "serveInline" -> delegate.serveInline();
        case "serveAsAttachment" -> delegate.serveAsAttachment();
        default -> throw new IllegalArgumentException(view);
        };

        assertEquals("icon.svg", wrapped.getUrlPostfix());
        assertTrue(wrapped.isAllowInert());
        assertTrue(wrapped.isRangeRequestsEnabled());
    }

    @Test
    void isRangeRequestsEnabled_onlyForFilesAndOptedInHandlers() {
        assertFalse(((DownloadHandler) event -> {
        }).isRangeRequestsEnabled());
        assertTrue(DownloadHandler.forFile(new File("video.mp4"))
                .isRangeRequestsEnabled());

        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        assertFalse(handler.isRangeRequestsEnabled());
        assertTrue(handler.enableRangeRequests().allowDisabled()
                .isRangeRequestsEnabled());
    }

    @Test
    void allowDisabled_abstractDownloadHandlerWrapped_modeIsAlways_inlinePreserved() {
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        handler.inline();

        DownloadHandler wrapped = handler.allowDisabled();

        assertEquals(DisabledUpdateMode.ALWAYS,
                wrapped.getDisabledUpdateMode());
        // The wrap does not interfere with the original handler's inline state
        assertTrue(handler.isInline());
    }

    @Test
    void allowDisabled_isIdempotent_returnsSameInstance() {
        DownloadHandler alwaysAllowed = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }

            @Override
            public DisabledUpdateMode getDisabledUpdateMode() {
                return DisabledUpdateMode.ALWAYS;
            }
        };

        assertSame(alwaysAllowed, alwaysAllowed.allowDisabled(),
                "allowDisabled() on a handler that is already ALWAYS must be a no-op");
    }

    @Test
    void allowDisabled_doubleWrap_returnsFirstWrapper() {
        DownloadHandler delegate = event -> {
        };
        DownloadHandler wrappedOnce = delegate.allowDisabled();
        DownloadHandler wrappedTwice = wrappedOnce.allowDisabled();

        assertSame(wrappedOnce, wrappedTwice,
                "Wrapping a handler that is already ALWAYS must be a no-op");
    }

    @Test
    void serveInline_handlerNamesAttachment_servedInlineWithFileName()
            throws Exception {
        DownloadHandler handler = event -> event.setFileName("q3.pdf");

        assertEquals("inline; filename=\"q3.pdf\"",
                servedContentDisposition(handler.serveInline()));
        assertEquals("attachment; filename=\"q3.pdf\"",
                servedContentDisposition(handler),
                "The original handler must not be changed");
    }

    @Test
    void serveAsAttachment_handlerAsksForInline_servedAsAttachment()
            throws Exception {
        DownloadHandler handler = event -> event.inline("q3.pdf");

        assertEquals("attachment; filename=\"q3.pdf\"",
                servedContentDisposition(handler.serveAsAttachment()));
    }

    @Test
    void serveAsAttachment_onInlineView_outermostViewDecides()
            throws Exception {
        DownloadHandler handler = DownloadEvent::inline;

        assertEquals("attachment", servedContentDisposition(
                handler.serveInline().allowDisabled().serveAsAttachment()));
        assertEquals("attachment", servedContentDisposition(
                handler.serveInline().serveAsAttachment()));
    }

    @Test
    void serveInline_keepsDisabledModeAndCombinesWithAllowDisabled() {
        DownloadHandler delegate = event -> {
        };

        DownloadHandler inline = delegate.serveInline();

        assertEquals(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                inline.getDisabledUpdateMode());
        assertEquals(DisabledUpdateMode.ALWAYS,
                delegate.allowDisabled().serveInline().getDisabledUpdateMode());
        assertSame(inline, inline.serveInline());
    }

    private static String servedContentDisposition(DownloadHandler handler)
            throws Exception {
        VaadinResponse response = mock(VaadinResponse.class);
        AtomicReference<String> header = new AtomicReference<>();
        doAnswer(invocation -> {
            header.set(invocation.getArgument(1));
            return null;
        }).when(response).setHeader(eq("Content-Disposition"), any());
        handler.handleRequest(mock(VaadinRequest.class), response,
                mock(VaadinSession.class), mock(Element.class));
        return header.get();
    }
}
