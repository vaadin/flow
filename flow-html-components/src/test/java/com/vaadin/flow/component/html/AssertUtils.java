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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadDisposition;
import com.vaadin.flow.server.streams.DownloadHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Assert utility class.
 */
class AssertUtils {

    /**
     * Asserts that two objects are equal. If they are not, an
     * {@link AssertionError} is thrown with the given message. If
     * <code>expected</code> and <code>actual</code> are <code>null</code>, they
     * are considered equal.
     *
     * If both <code>expected</code> and <code>actual</code> are arrays or are
     * <code>Optional</code>s wrapping arrays,
     * <code>Assertions#assertArrayEquals</code> is used to assert that the
     * arrays content is equal.
     *
     * @param expected
     *            expected value
     * @param actual
     *            actual value
     * @param message
     *            the identifying message for the {@link AssertionError}
     *            (<code>null</code> okay)
     */
    static void assertEquals(Object expected, Object actual, String message) {

        if (expected instanceof Optional && actual instanceof Optional) {
            expected = ((Optional) expected).orElseGet(() -> null);
            actual = ((Optional) actual).orElseGet(() -> null);
        }

        if (expected == actual) {
            // Null check.
            return;

        } else if (expected == null || actual == null) {
            Assertions.assertEquals(expected, actual, message);
        }

        if (expected.getClass().isArray() && actual.getClass().isArray()) {
            assertArrayEquals((Object[]) expected, (Object[]) actual, message);

        } else {
            Assertions.assertEquals(expected, actual, message);
        }
    }

    /**
     * Serves a request with the given handler and returns the
     * <code>Content-Disposition</code> header of the response.
     *
     * @param handler
     *            the handler as the component registered it
     * @return the header value, or <code>null</code> if it was not set
     */
    static String servedContentDisposition(DownloadHandler handler)
            throws IOException {
        return servedContentDisposition(handler, null);
    }

    /**
     * Serves a request with the handler of the given resource, with the
     * disposition of the resource as the request handler passes it, and returns
     * the <code>Content-Disposition</code> header of the response.
     *
     * @param resource
     *            the resource as the component registered it
     * @return the header value, or <code>null</code> if it was not set
     */
    static String servedContentDisposition(ElementStreamResource resource)
            throws IOException {
        return servedContentDisposition(
                (DownloadHandler) resource.getElementRequestHandler(),
                resource.getDisposition());
    }

    private static String servedContentDisposition(DownloadHandler handler,
            DownloadDisposition disposition) throws IOException {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getAttribute(DownloadDisposition.class.getName()))
                .thenReturn(disposition);
        VaadinResponse response = mock(VaadinResponse.class);
        AtomicReference<String> header = new AtomicReference<>();
        doAnswer(invocation -> {
            header.set(invocation.getArgument(1));
            return null;
        }).when(response).setHeader(eq("Content-Disposition"), any());
        handler.handleRequest(request, response, mock(VaadinSession.class),
                mock(Element.class));
        return header.get();
    }
}
