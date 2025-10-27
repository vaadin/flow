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
package com.vaadin.flow.component.html;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

public class ImageTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
    }

    @Test
    @Override
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    public void emptyAltKeepsAttribute() {
        Image img = new Image("test.png", "");
        Assert.assertEquals("", img.getAlt().get());
        Assert.assertTrue(img.getElement().hasAttribute("alt"));
        img.setAlt(null);
        Assert.assertEquals(Optional.empty(), img.getAlt());
        Assert.assertFalse(img.getElement().hasAttribute("alt"));
    }

    @Test
    public void downloadHandler_isSetToInline() {
        Element element = Mockito.mock(Element.class);
        class TestImage extends Image {
            public TestImage(DownloadHandler downloadHandler, String alt) {
                super(downloadHandler, alt);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }
        // dummy handler
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        Assert.assertFalse(handler.isInline());
        new TestImage(handler, "test.png");
        Assert.assertTrue(handler.isInline());
    }

    /**
     * Helper method to capture and invoke a DownloadHandler, returning the
     * captured content type.
     */
    private String captureAndInvokeDownloadHandler(Element element)
            throws Exception {
        ArgumentCaptor<DownloadHandler> handlerCaptor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        Mockito.verify(element).setAttribute(Mockito.eq("src"),
                handlerCaptor.capture());

        DownloadHandler handler = handlerCaptor.getValue();
        Assert.assertTrue("Handler should be InputStreamDownloadHandler",
                handler instanceof InputStreamDownloadHandler);

        // Create mock event and response to capture content type
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        OutputStream outputStream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getMimeType(Mockito.anyString()))
                .thenReturn("application/octet-stream");

        DownloadEvent event = new DownloadEvent(request, response, session,
                element);
        handler.handleDownloadRequest(event);

        ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor
                .forClass(String.class);
        Mockito.verify(response).setContentType(contentTypeCaptor.capture());
        return contentTypeCaptor.getValue();
    }

    @Test
    public void byteArrayConstructor_typicalUseCase() throws Exception {
        Element element = Mockito.mock(Element.class);
        byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };

        class TestImage extends Image {
            public TestImage(byte[] content, String name) {
                super(content, name);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }

        new TestImage(imageData, "test.png");
        Mockito.verify(element).setAttribute("alt", "test.png");

        String contentType = captureAndInvokeDownloadHandler(element);
        Assert.assertEquals("image/png", contentType);
    }

    @Test
    public void byteArrayConstructor_withExplicitMimeType() throws Exception {
        Element element = Mockito.mock(Element.class);
        byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };

        class TestImage extends Image {
            public TestImage(byte[] content, String name, String mimeType) {
                super(content, name, mimeType);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }

        new TestImage(imageData, "test.webp", "image/webp");
        Mockito.verify(element).setAttribute("alt", "test.webp");

        String contentType = captureAndInvokeDownloadHandler(element);
        Assert.assertEquals("image/webp", contentType);
    }

    @Test
    public void byteArrayConstructor_withNullMimeType() throws Exception {
        Element element = Mockito.mock(Element.class);
        byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };

        class TestImage extends Image {
            public TestImage(byte[] content, String name, String mimeType) {
                super(content, name, mimeType);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }

        new TestImage(imageData, "test.img", null);
        Mockito.verify(element).setAttribute("alt", "test.img");

        String contentType = captureAndInvokeDownloadHandler(element);
        // When MIME type is null, it falls back to the service's getMimeType
        Assert.assertEquals("application/octet-stream", contentType);
    }
}
