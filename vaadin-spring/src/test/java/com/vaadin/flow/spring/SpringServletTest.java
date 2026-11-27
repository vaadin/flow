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
package com.vaadin.flow.spring;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpringServletTest {

    private final HttpServletResponse response = mock(
            HttpServletResponse.class);
    private final ServletOutputStream outputStream = mock(
            ServletOutputStream.class);
    private final PrintWriter writer = mock(PrintWriter.class);

    @Test
    void forward_closesOutputStreamThroughGivenResponse() throws IOException {
        when(response.getOutputStream()).thenReturn(outputStream);

        SpringServlet.closeResponseAfterForward(
                request(DispatcherType.FORWARD, false), response);

        verify(outputStream).close();
        verify(response, never()).getWriter();
    }

    @Test
    void forwardWrittenWithWriter_closesWriter() throws IOException {
        when(response.getOutputStream())
                .thenThrow(new IllegalStateException("writer in use"));
        when(response.getWriter()).thenReturn(writer);

        SpringServlet.closeResponseAfterForward(
                request(DispatcherType.FORWARD, false), response);

        verify(writer).close();
    }

    @Test
    void forwardToDisconnectedClient_doesNotThrow() throws IOException {
        when(response.getOutputStream()).thenReturn(outputStream);
        doThrow(new IOException("client went away")).when(outputStream).close();

        SpringServlet.closeResponseAfterForward(
                request(DispatcherType.FORWARD, false), response);

        verify(outputStream).close();
    }

    @Test
    void asyncForward_leavesResponseOpen() {
        SpringServlet.closeResponseAfterForward(
                request(DispatcherType.FORWARD, true), response);

        verifyNoInteractions(response);
    }

    @Test
    void directRequest_leavesResponseOpen() {
        SpringServlet.closeResponseAfterForward(
                request(DispatcherType.REQUEST, false), response);

        verifyNoInteractions(response);
    }

    private static HttpServletRequest request(DispatcherType type,
            boolean asyncStarted) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getDispatcherType()).thenReturn(type);
        when(request.isAsyncStarted()).thenReturn(asyncStarted);
        return request;
    }
}
