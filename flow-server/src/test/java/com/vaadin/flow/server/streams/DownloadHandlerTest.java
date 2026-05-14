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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        DownloadEvent event = new DownloadEvent(
                Mockito.mock(VaadinRequest.class),
                Mockito.mock(VaadinResponse.class),
                Mockito.mock(VaadinSession.class), Mockito.mock(Element.class));

        delegate.allowDisabled().handleDownloadRequest(event);

        assertSame(event, received.get(),
                "Wrapped handler must forward the event to the delegate");
    }

    @Test
    void allowDisabled_forwardsUrlPostfixAndAllowInert() {
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
        };

        DownloadHandler wrapped = delegate.allowDisabled();

        assertEquals("icon.svg", wrapped.getUrlPostfix());
        assertTrue(wrapped.isAllowInert());
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
}
