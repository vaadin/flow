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
package com.vaadin.flow.server.communication;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartbeatHandlerTest {

    @Test
    public void synchronizedHandleRequest_uiPresent_setLastHeartbeatTimestampIsCalledOnce()
            throws IOException {
        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        VaadinRequest request = mock(VaadinRequest.class);
        VaadinResponse response = mock(VaadinResponse.class);
        UI ui = mock(UI.class);
        UIInternals uiInternals = mock(UIInternals.class);

        when(ui.getInternals()).thenReturn(uiInternals);
        when(session.getService()).thenReturn(service);
        when(service.findUI(request)).thenReturn(ui);

        HeartbeatHandler handler = new HeartbeatHandler();
        handler.synchronizedHandleRequest(session, request, response);

        Mockito.verify(ui.getInternals(), times(1))
                .setLastHeartbeatTimestamp(anyLong());
    }

    @Test
    public void synchronizedHandleRequest_uiPresent_noCacheHeaderSetAndContentTypeNotSet()
            throws IOException {
        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        VaadinRequest request = mock(VaadinRequest.class);
        VaadinResponse response = mock(VaadinResponse.class);
        UI ui = mock(UI.class);
        UIInternals uiInternals = mock(UIInternals.class);

        when(ui.getInternals()).thenReturn(uiInternals);
        when(session.getService()).thenReturn(service);
        when(service.findUI(request)).thenReturn(ui);

        HeartbeatHandler handler = new HeartbeatHandler();
        handler.synchronizedHandleRequest(session, request, response);

        // Verify Cache-Control header is set
        verify(response, times(1)).setHeader(eq("Cache-Control"),
                eq("no-cache"));

        // Verify Content-Type header is NOT set
        verify(response, never()).setHeader(eq("Content-Type"),
                Mockito.anyString());
    }
}
