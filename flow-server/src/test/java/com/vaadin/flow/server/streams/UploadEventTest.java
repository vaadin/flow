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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Unit tests for {@link UploadEvent} rejection functionality.
 */
public class UploadEventTest {

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private Element owner;

    @Before
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

        Assert.assertFalse("Event should not be rejected initially",
                event.isRejected());
        Assert.assertNull("Rejection message should be null initially",
                event.getRejectionMessage());
    }

    @Test
    public void testReject_withDefaultMessage() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject();

        Assert.assertTrue("Event should be marked as rejected",
                event.isRejected());
        Assert.assertEquals("Default rejection message should be set",
                "File rejected", event.getRejectionMessage());
    }

    @Test
    public void testReject_withCustomMessage() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.zip", 100L, "application/zip", owner, null);

        String customMessage = "Only PNG files are accepted";
        event.reject(customMessage);

        Assert.assertTrue("Event should be marked as rejected",
                event.isRejected());
        Assert.assertEquals("Custom rejection message should be set",
                customMessage, event.getRejectionMessage());
    }

    @Test
    public void testReject_canBeCalledMultipleTimes() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject("First reason");
        Assert.assertTrue("Event should be marked as rejected",
                event.isRejected());
        Assert.assertEquals("First rejection message should be set",
                "First reason", event.getRejectionMessage());

        // Call reject again with different message
        event.reject("Second reason");
        Assert.assertTrue("Event should still be marked as rejected",
                event.isRejected());
        Assert.assertEquals("Rejection message should be updated",
                "Second reason", event.getRejectionMessage());
    }

    @Test
    public void testGetFileName_returnsCorrectFileName() {
        String fileName = "document.pdf";
        UploadEvent event = new UploadEvent(request, response, session,
                fileName, 1000L, "application/pdf", owner, null);

        Assert.assertEquals("File name should match", fileName,
                event.getFileName());
    }

    @Test
    public void testReject_withNullMessage_setsNullMessage() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject(null);

        Assert.assertTrue("Event should be marked as rejected",
                event.isRejected());
        Assert.assertNull("Rejection message should be null",
                event.getRejectionMessage());
    }

    @Test
    public void testReject_withEmptyMessage_setsEmptyMessage() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject("");

        Assert.assertTrue("Event should be marked as rejected",
                event.isRejected());
        Assert.assertEquals("Rejection message should be empty", "",
                event.getRejectionMessage());
    }

    @Test
    public void testGetInputStream_rejectedUpload_throwsException() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        event.reject("Not allowed");

        try {
            event.getInputStream();
            Assert.fail(
                    "Expected IllegalStateException when accessing rejected upload stream");
        } catch (IllegalStateException e) {
            Assert.assertTrue("Exception should mention rejection",
                    e.getMessage().contains("rejected"));
            Assert.assertTrue("Exception should include rejection reason",
                    e.getMessage().contains("Not allowed"));
        }
    }

    @Test
    public void testGetInputStream_beforeRejection_works() {
        UploadEvent event = new UploadEvent(request, response, session,
                "test.txt", 100L, "text/plain", owner, null);

        Assert.assertNotNull("Should be able to get input stream",
                event.getInputStream());
    }
}
