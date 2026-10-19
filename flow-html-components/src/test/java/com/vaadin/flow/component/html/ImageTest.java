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
package com.vaadin.flow.component.html;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    void emptyAltKeepsAttribute() {
        Image img = new Image("test.png", "");
        assertEquals("", img.getAlt().get());
        assertTrue(img.getElement().hasAttribute("alt"));
        img.setAlt(null);
        assertEquals(Optional.empty(), img.getAlt());
        assertFalse(img.getElement().hasAttribute("alt"));
    }

    @Test
    void setSrc_downloadHandler_disabledUpdateModeIsAlways() {
        Element element = mock(Element.class);
        class TestImage extends Image {
            @Override
            public Element getElement() {
                return element;
            }
        }
        // Plain lambda DownloadHandler, not an AbstractDownloadHandler subclass
        DownloadHandler lambda = event -> {
        };

        new TestImage().setSrc(lambda);

        ArgumentCaptor<DownloadHandler> captor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        verify(element).setAttribute(eq("src"), captor.capture());
        assertEquals(DisabledUpdateMode.ALWAYS,
                captor.getValue().getDisabledUpdateMode());
    }

    @Test
    void downloadHandler_isSetToInline() {
        Element element = mock(Element.class);
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
        assertFalse(handler.isInline());
        new TestImage(handler, "test.png");
        assertTrue(handler.isInline());
    }

    /**
     * Helper method to capture and invoke a DownloadHandler, returning the
     * captured content type.
     */
    private String captureAndInvokeDownloadHandler(Element element)
            throws Exception {
        ArgumentCaptor<DownloadHandler> handlerCaptor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        verify(element).setAttribute(eq("src"), handlerCaptor.capture());

        DownloadHandler handler = handlerCaptor.getValue();
        assertEquals(DisabledUpdateMode.ALWAYS, handler.getDisabledUpdateMode(),
                "Handler set on the image must allow disabled, so the browser can still load the image when the component is disabled");

        // Create mock event and response to capture content type
        VaadinRequest request = mock(VaadinRequest.class);
        VaadinResponse response = mock(VaadinResponse.class);
        VaadinSession session = mock(VaadinSession.class);
        VaadinService service = mock(VaadinService.class);
        OutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        DownloadEvent event = new DownloadEvent(request, response, session,
                element);
        handler.handleDownloadRequest(event);

        ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor
                .forClass(String.class);
        verify(response).setContentType(contentTypeCaptor.capture());
        return contentTypeCaptor.getValue();
    }

    @Test
    void byteArrayConstructor_typicalUseCase() throws Exception {
        Element element = mock(Element.class);
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
        verify(element).setAttribute("alt", "test.png");

        String contentType = captureAndInvokeDownloadHandler(element);
        assertEquals("image/png", contentType);
    }

    @Test
    void byteArrayConstructor_withExplicitMimeType() throws Exception {
        Element element = mock(Element.class);
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
        verify(element).setAttribute("alt", "test.webp");

        String contentType = captureAndInvokeDownloadHandler(element);
        assertEquals("image/webp", contentType);
    }

    @Test
    void byteArrayConstructor_withNullMimeType() throws Exception {
        Element element = mock(Element.class);
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
        verify(element).setAttribute("alt", "test.img");

        String contentType = captureAndInvokeDownloadHandler(element);
        // When MIME type is null, it falls back to the service's getMimeType
        assertEquals("application/octet-stream", contentType);
    }
}
