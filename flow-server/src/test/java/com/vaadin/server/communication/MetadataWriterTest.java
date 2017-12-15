/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.communication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.communication.MetadataWriter;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

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
        JsonObject meta = new MetadataWriter().createMetadata(ui, false, true,
                messages);
        Assert.assertEquals("{\"async\":true}", meta.toJson());
    }

    @Test
    public void writeRepaintTag() throws Exception {
        JsonObject meta = new MetadataWriter().createMetadata(ui, true, false,
                messages);
        Assert.assertEquals("{\"repaintAll\":true}", meta.toJson());
    }

    @Test
    public void writeRepaintAndAsyncTag() throws Exception {
        JsonObject meta = new MetadataWriter().createMetadata(ui, true, true,
                messages);
        Assert.assertEquals("{\"repaintAll\":true,\"async\":true}",
                meta.toJson());
    }

    @Test
    public void writeRedirectWithExpiredSession() throws Exception {
        disableSessionExpirationMessages(messages);

        JsonObject meta = new MetadataWriter().createMetadata(ui, false, false,
                messages);
        Assert.assertEquals("{}", meta.toJson());
    }

    @Test
    public void writeRedirectWithActiveSession() throws Exception {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        when(session.getSession()).thenReturn(wrappedSession);

        disableSessionExpirationMessages(messages);

        JsonObject meta = new MetadataWriter().createMetadata(ui, false, false,
                messages);
        Assert.assertEquals(
                "{\"timedRedirect\":{\"interval\":15,\"url\":\"\"}}",
                meta.toJson());
    }

    @Test
    public void writeAsyncWithSystemMessages() throws IOException {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        when(session.getSession()).thenReturn(wrappedSession);

        disableSessionExpirationMessages(messages);

        JsonObject meta = new MetadataWriter().createMetadata(ui, false, true,
                messages);
        Assert.assertEquals(
                "{\"async\":true,\"timedRedirect\":{\"interval\":15,\"url\":\"\"}}",
                meta.toJson());
    }
}
