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
package com.vaadin.flow.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.shared.ApplicationConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnsupportedBrowserHandlerTest {

    private final VaadinService service = new MockVaadinServletService();
    private final VaadinSession session = new MockVaadinSession(service);
    private final VaadinResponse response = mock(VaadinResponse.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final List<UnsupportedBrowserEvent> events = new ArrayList<>();

    UnsupportedBrowserHandlerTest() throws IOException {
        when(response.getOutputStream()).thenReturn(output);
        service.getEventBus().addListener(UnsupportedBrowserEvent.class,
                events::add);
    }

    @Test
    void browserTooOldRequest_eventFiredAndPageWritten() throws IOException {
        VaadinRequest request = createRequest(
                RequestType.BROWSER_TOO_OLD.getIdentifier());

        assertTrue(new UnsupportedBrowserHandler().handleRequest(session,
                request, response));

        assertEquals(1, events.size());
        UnsupportedBrowserEvent event = events.get(0);
        assertSame(service, event.getService());
        assertSame(session, event.getSession());
        assertSame(request, event.getRequest());
        assertTrue(output.toString().contains("<html"));
    }

    @Test
    void otherRequest_noEventFired() throws IOException {
        VaadinRequest request = createRequest(RequestType.UIDL.getIdentifier());

        assertFalse(new UnsupportedBrowserHandler().handleRequest(session,
                request, response));

        assertTrue(events.isEmpty());
    }

    private static VaadinRequest createRequest(String requestType) {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(requestType);
        return request;
    }
}
