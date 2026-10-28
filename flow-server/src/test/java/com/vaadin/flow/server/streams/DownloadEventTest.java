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
package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.internal.EncodeUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadEventTest {
    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;

    @BeforeEach
    void setUp() throws IOException {
        request = mock(VaadinRequest.class);
        response = mock(VaadinResponse.class);
        session = mock(VaadinSession.class);
    }

    static Stream<Arguments> asciiFileNames() {
        return Stream.of(Arguments.of("test.txt", "test.txt"),
                Arguments.of("a.pdf\"; filename*=UTF-8''b.exe",
                        "a.pdf\\\"; filename*=UTF-8''b.exe"),
                Arguments.of("a\\b\r\n.txt", "a\\\\b.txt"));
    }

    @ParameterizedTest
    @MethodSource("asciiFileNames")
    void setFileName_asciiFileName_setsContentDispositionFilenameQuotedToResponse(
            String fileName, String quotedContent) {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName(fileName);
        verify(response).setHeader("Content-Disposition",
                "attachment; filename=\"" + quotedContent + "\"");
    }

    @Test
    void setFileName_nonEmptyFileName_setsContentDispositionEncodedFilenameQuotedToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "test üñîçødë.txt";
        downloadEvent.setFileName(fileName);
        verify(response).setHeader("Content-Disposition", "attachment;"
                + " filename=\"" + EncodeUtil.rfc2047Encode(fileName) + "\";"
                + " filename*=UTF-8''" + EncodeUtil.rfc5987Encode(fileName));
    }

    @Test
    void setFileName_emptyFileName_setsContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "";
        downloadEvent.setFileName(fileName);
        verify(response).setHeader("Content-Disposition", "attachment");
    }

    @ParameterizedTest
    @ValueSource(strings = { "   ", "\u0000\r\n" })
    void setFileName_blankFileName_setsContentDispositionToResponse(
            String fileName) {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName(fileName);
        verify(response).setHeader("Content-Disposition", "attachment");
    }

    @Test
    void setFileName_nullFileName_doesNotSetContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName(null);
        verify(response, times(0)).setHeader(anyString(), anyString());
    }

    @Test
    void setFileName_headerAlreadySet_doesNotOverrideHeader() {
        when(response.containsHeader("Content-Disposition")).thenReturn(true);
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName("test.txt");
        verify(response, times(0)).setHeader(eq("Content-Disposition"),
                anyString());
    }

    @Test
    void inline_noFileName_setsContentDispositionInlineToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline();
        verify(response).setHeader("Content-Disposition", "inline");
    }

    @ParameterizedTest
    @MethodSource("asciiFileNames")
    void inline_asciiFileName_setsContentDispositionInlineWithFilenameToResponse(
            String fileName, String quotedContent) {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline(fileName);
        verify(response).setHeader("Content-Disposition",
                "inline; filename=\"" + quotedContent + "\"");
    }

    @Test
    void inline_withNonASCIIFileName_setsContentDispositionInlineWithEncodedFilenameToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "dökümänt üñîçødë.pdf";
        downloadEvent.inline(fileName);
        verify(response).setHeader("Content-Disposition",
                "inline;" + " filename=\"" + EncodeUtil.rfc2047Encode(fileName)
                        + "\";" + " filename*=UTF-8''"
                        + EncodeUtil.rfc5987Encode(fileName));
    }

    @Test
    void inline_nullFileName_setsContentDispositionInlineToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline(null);
        verify(response).setHeader("Content-Disposition", "inline");
    }

    @Test
    void inline_emptyFileName_setsContentDispositionInlineToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline("");
        verify(response).setHeader("Content-Disposition", "inline");
    }

    @ParameterizedTest
    @ValueSource(strings = { "   ", "\u0000\r\n" })
    void inline_blankFileName_setsContentDispositionInlineToResponse(
            String fileName) {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline(fileName);
        verify(response).setHeader("Content-Disposition", "inline");
    }

    @Test
    void inline_headerAlreadySet_doesNotOverrideHeader() {
        when(response.containsHeader("Content-Disposition")).thenReturn(true);
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline();
        verify(response, times(0)).setHeader(eq("Content-Disposition"),
                anyString());
    }

    @Test
    void inline_withFileNameHeaderAlreadySet_doesNotOverrideHeader() {
        when(response.containsHeader("Content-Disposition")).thenReturn(true);
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.inline("test.pdf");
        verify(response, times(0)).setHeader(eq("Content-Disposition"),
                anyString());
    }

    @Test
    void setContentType_setsContentTypeToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String contentType = "application/pdf";
        downloadEvent.setContentType(contentType);
        verify(response).setContentType(contentType);
    }

    @Test
    void setContentLenght_setsContentLengthToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        int contentLength = 1024;
        downloadEvent.setContentLength(contentLength);
        verify(response).setContentLengthLong(contentLength);
    }

    @Test
    void setContentLenght_unknownLength_doesNotSetContentLengthToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        int contentLength = -1;
        downloadEvent.setContentLength(contentLength);
        verify(response, times(0)).setContentLengthLong(contentLength);
    }
}
