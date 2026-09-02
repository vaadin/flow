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
package com.vaadin.flow.internal.streams;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.tests.util.TestServletStreams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the request and response streams that an active transfer hands out.
 */
class ActiveTransferTest {

    private static final byte[] CHUNK = "0123456789"
            .getBytes(StandardCharsets.UTF_8);

    private MockVaadinServletService service;
    private ActiveTransfer transfer;

    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;
    private ByteArrayOutputStream writtenBytes;
    private StringWriter writtenText;

    @BeforeEach
    void setUp() throws IOException {
        service = new MockVaadinServletService();
        transfer = new ActiveTransfer("VAADIN/dynamic/resource/0/key/file.bin",
                new Element("a"));

        httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpRequest.getInputStream())
                .thenReturn(TestServletStreams.inputStream(CHUNK));
        Mockito.when(httpRequest.getReader())
                .thenReturn(new BufferedReader(new StringReader("0123456789")));

        httpResponse = Mockito.mock(HttpServletResponse.class);
        writtenBytes = new ByteArrayOutputStream();
        Mockito.when(httpResponse.getOutputStream())
                .thenReturn(TestServletStreams.outputStream(writtenBytes));
        writtenText = new StringWriter();
        Mockito.when(httpResponse.getWriter())
                .thenReturn(new PrintWriter(writtenText));
    }

    @Test
    void outputStream_terminated_refusesToWriteMore() throws IOException {
        OutputStream outputStream = wrapResponse().getOutputStream();
        outputStream.write(CHUNK);
        outputStream.write(CHUNK[0]);

        assertFalse(transfer.isTerminated());

        transfer.terminate();

        assertTrue(transfer.isTerminated());

        assertThrows(IOException.class, () -> outputStream.write(CHUNK),
                "A terminated transfer should not write more bytes");
        assertEquals(CHUNK.length + 1, writtenBytes.size(),
                "Nothing should have reached the response after termination");
    }

    @Test
    void writer_terminated_refusesToWriteMore() throws IOException {
        PrintWriter writer = wrapResponse().getWriter();
        writer.write("first");
        writer.flush();

        transfer.terminate();

        writer.write("second");
        writer.flush();

        assertEquals("first", writtenText.toString(),
                "Nothing should have reached the response after termination");
        assertTrue(writer.checkError(),
                "The writer should report the failure of a terminated transfer");
        writer.close();
    }

    @Test
    void inputStream_terminated_refusesToReadMore() throws IOException {
        InputStream inputStream = wrapRequest().getInputStream();
        assertEquals(CHUNK[0], inputStream.read());
        assertEquals(CHUNK.length - 1,
                inputStream.read(new byte[CHUNK.length]));
        assertEquals(-1, inputStream.read(), "The content should be consumed");

        transfer.terminate();

        assertThrows(IOException.class, inputStream::read,
                "A terminated transfer should not read more bytes");
    }

    @Test
    void reader_terminated_refusesToReadMore() throws IOException {
        BufferedReader reader = wrapRequest().getReader();
        assertEquals("0123456789", reader.readLine());

        transfer.terminate();

        assertThrows(IOException.class, reader::read,
                "A terminated transfer should not read more characters");
        reader.close();
    }

    @Test
    void parts_terminated_partStreamRefusesToReadMore()
            throws IOException, ServletException {
        Part part = Mockito.mock(Part.class);
        Mockito.when(part.getSubmittedFileName()).thenReturn("file.bin");
        Mockito.when(part.getInputStream())
                .thenReturn(new ByteArrayInputStream(CHUNK));
        Mockito.when(httpRequest.getParts()).thenReturn(List.of(part));

        Part wrappedPart = wrapRequest().getParts().iterator().next();

        InputStream partStream = wrappedPart.getInputStream();
        assertEquals(CHUNK.length, partStream.read(new byte[CHUNK.length]));

        transfer.terminate();

        assertThrows(IOException.class, partStream::read,
                "A terminated transfer should not read more of a part");
    }

    @Test
    void part_everythingButTheContent_delegated()
            throws IOException, ServletException {
        Part part = Mockito.mock(Part.class);
        Mockito.when(part.getSubmittedFileName()).thenReturn("file.bin");
        Mockito.when(part.getName()).thenReturn("file");
        Mockito.when(part.getContentType()).thenReturn("text/plain");
        Mockito.when(part.getSize()).thenReturn(10L);
        Mockito.when(part.getHeader("Content-Type")).thenReturn("text/plain");
        Mockito.when(part.getHeaders("Content-Type"))
                .thenReturn(List.of("text/plain"));
        Mockito.when(part.getHeaderNames()).thenReturn(List.of("Content-Type"));
        Mockito.when(httpRequest.getPart("file")).thenReturn(part);

        Part wrappedPart = wrapRequest().getPart("file");

        assertEquals("file.bin", wrappedPart.getSubmittedFileName());
        assertEquals("file", wrappedPart.getName());
        assertEquals("text/plain", wrappedPart.getContentType());
        assertEquals(10L, wrappedPart.getSize());
        assertEquals("text/plain", wrappedPart.getHeader("Content-Type"));
        assertEquals(List.of("text/plain"),
                wrappedPart.getHeaders("Content-Type"));
        assertEquals(List.of("Content-Type"), wrappedPart.getHeaderNames());

        wrappedPart.write("target.bin");
        Mockito.verify(part).write("target.bin");
        wrappedPart.delete();
        Mockito.verify(part).delete();

        assertNull(wrapRequest().getPart("missing"),
                "A part that does not exist should stay null");
    }

    @Test
    void servletStreams_everythingButTheContent_delegated() throws IOException {
        ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);
        Mockito.when(inputStream.isReady()).thenReturn(true);
        Mockito.when(inputStream.isFinished()).thenReturn(true);
        Mockito.when(inputStream.available()).thenReturn(42);
        Mockito.when(httpRequest.getInputStream()).thenReturn(inputStream);

        ServletInputStream wrappedInput = wrapRequest().getInputStream();
        assertTrue(wrappedInput.isReady());
        assertTrue(wrappedInput.isFinished());
        assertEquals(42, wrappedInput.available());
        ReadListener readListener = Mockito.mock(ReadListener.class);
        wrappedInput.setReadListener(readListener);
        Mockito.verify(inputStream).setReadListener(readListener);
        wrappedInput.close();
        Mockito.verify(inputStream).close();

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(outputStream.isReady()).thenReturn(true);
        Mockito.when(httpResponse.getOutputStream()).thenReturn(outputStream);

        ServletOutputStream wrappedOutput = wrapResponse().getOutputStream();
        assertTrue(wrappedOutput.isReady());
        WriteListener writeListener = Mockito.mock(WriteListener.class);
        wrappedOutput.setWriteListener(writeListener);
        Mockito.verify(outputStream).setWriteListener(writeListener);
        wrappedOutput.flush();
        Mockito.verify(outputStream).flush();
        wrappedOutput.close();
        Mockito.verify(outputStream).close();
    }

    @Test
    void nonServletRequestAndResponse_leftUnwrapped() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        assertSame(request, transfer.wrapRequest(request),
                "A request that cannot be wrapped should be returned as-is");
        assertSame(response, transfer.wrapResponse(response),
                "A response that cannot be wrapped should be returned as-is");
    }

    @Test
    void description_containsPathAndOwner() {
        String description = transfer.getDescription();

        assertTrue(description.contains("file.bin"),
                "Description should contain the path: " + description);
        assertTrue(description.contains("a"),
                "Description should contain the owner: " + description);
    }

    private VaadinServletRequest wrapRequest() {
        return (VaadinServletRequest) transfer
                .wrapRequest(new VaadinServletRequest(httpRequest, service));
    }

    private VaadinServletResponse wrapResponse() {
        return (VaadinServletResponse) transfer
                .wrapResponse(new VaadinServletResponse(httpResponse, service));
    }

}
