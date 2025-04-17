/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import com.vaadin.flow.server.WrappedSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataWriterTest {

    private UI ui;
    private VaadinSession session;
    private SystemMessages messages;

    @Before
    public void setup() {
        ui = Mockito.mock(UI.class);
        session = Mockito.mock(VaadinSession.class);
        Mockito.when(ui.getSession()).thenReturn(session);
        messages = Mockito.mock(SystemMessages.class);
    }

    private void disableSessionExpirationMessages(SystemMessages messages) {
        when(messages.isSessionExpiredNotificationEnabled()).thenReturn(true);
        when(messages.getSessionExpiredMessage()).thenReturn(null);
        when(messages.getSessionExpiredCaption()).thenReturn(null);
    }

    @Test
    public void writeAsyncTag() throws Exception {
        assertMetadataOutput(false, true, "{\"async\":true}");
    }

    @Test
    public void writeRepaintTag() throws Exception {
        assertMetadataOutput(true, false, "{\"repaintAll\":true}");
    }

    @Test
    public void writeRepaintAndAsyncTag() throws Exception {
        assertMetadataOutput(true, true,
                "{\"repaintAll\":true,\"async\":true}");
    }

    @Test
    public void writeRedirectWithExpiredSession() throws Exception {
        disableSessionExpirationMessages(messages);

        assertMetadataOutput(false, false, "{}");
    }

    @Test
    public void writeRedirectWithActiveSession() throws Exception {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        when(session.getSession()).thenReturn(wrappedSession);

        disableSessionExpirationMessages(messages);

        assertMetadataOutput(false, false,
                "{\"timedRedirect\":{\"interval\":15,\"url\":\"\"}}");
    }

    @Test
    public void writeAsyncWithSystemMessages() throws IOException {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        when(session.getSession()).thenReturn(wrappedSession);

        disableSessionExpirationMessages(messages);

        assertMetadataOutput(false, true,
                "{\"async\":true,\"timedRedirect\":{\"interval\":15,\"url\":\"\"}}");
    }

    @Test
    public void writeSessionExpiredTag_sessionIsOpen() throws Exception {
        Mockito.when(session.getState()).thenReturn(VaadinSessionState.OPEN);
        assertMetadataOutput(false, false, "{}");
    }

    @Test
    public void writeSessionExpiredTag_sessionIsClosing() throws Exception {
        Mockito.when(session.getState()).thenReturn(VaadinSessionState.CLOSING);
        assertMetadataOutput(false, false, "{\"sessionExpired\":true}");

        Mockito.when(session.getState()).thenReturn(VaadinSessionState.CLOSED);
        assertMetadataOutput(false, false, "{\"sessionExpired\":true}");
    }

    @Test
    public void writeSessionExpiredTag_sessionIsClosed() throws Exception {
        Mockito.when(session.getState()).thenReturn(VaadinSessionState.CLOSED);
        assertMetadataOutput(false, false, "{\"sessionExpired\":true}");
    }

    private void assertMetadataOutput(boolean repaintAll, boolean async,
            String expectedOutput) {
        ObjectNode meta = new MetadataWriter().createMetadata(ui, repaintAll,
                async, messages);
        Assert.assertEquals(expectedOutput, meta.toString());
    }

}
