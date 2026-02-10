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
package com.vaadin.flow.server.streams;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Unit tests for {@link UploadEvent} rejection functionality.
 */
class UploadEventTest {

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private Element owner;

    @BeforeEach
    public void setUp() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        session = Mockito.mock(VaadinSession.class);
        owner = Mockito.mock(Element.class);

        Mockito.when(request.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[0]));
    }

    @Test
    public void testInitialState_notRejected() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        Assertions.assertFalse(event.isRejected(),
                "Event should not be rejected initially");
        Assertions.assertNull(event.getRejectionMessage(),
                "Rejection message should be null initially");
    }

    @Test
    public void testReject_withDefaultMessage() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject();

        Assertions.assertTrue(event.isRejected(),
                "Event should be marked as rejected");
        Assertions.assertEquals("File rejected", event.getRejectionMessage(),
                "Default rejection message should be set");
    }

    @Test
    public void testReject_withCustomMessage() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.zip", 100L, "application/zip", owner, null);

        String customMessage = "Only PNG files are accepted";
        event.reject(customMessage);

        Assertions.assertTrue(event.isRejected(),
                "Event should be marked as rejected");
        Assertions.assertEquals(customMessage, event.getRejectionMessage(),
                "Custom rejection message should be set");
    }

    @Test
    public void testGetInputStream_rejectedUpload_throwsException() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject("Not allowed");

        try {
            event.getInputStream();
            Assertions.fail(
                    "Expected IllegalStateException when accessing rejected upload stream");
        } catch (IllegalStateException e) {
            Assertions.assertTrue(e.getMessage().contains("rejected"),
                    "Exception should mention rejection");
            Assertions.assertTrue(e.getMessage().contains("Not allowed"),
                    "Exception should include rejection reason");
        }
    }

    @Test
    public void testGetInputStream_beforeRejection_works() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        Assertions.assertNotNull(event.getInputStream(),
                "Should be able to get input stream");
    }
}
