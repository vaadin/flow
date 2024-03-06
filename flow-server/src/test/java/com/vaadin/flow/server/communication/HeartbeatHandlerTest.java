/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class HeartbeatHandlerTest {

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
}
