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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.internal.EncodeUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("removal")
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
                Arguments.of("a\\b\r\n\t.txt", "a\\\\b.txt"));
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
    void setFileName_nonAsciiFileNameWithControlCharacters_controlCharactersRemoved() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName("ä\r\n\u0085.txt");
        verify(response).setHeader("Content-Disposition", "attachment;"
                + " filename=\"" + EncodeUtil.rfc2047Encode("ä.txt") + "\";"
                + " filename*=UTF-8''" + EncodeUtil.rfc5987Encode("ä.txt"));
        assertEquals("ä.txt", downloadEvent.getFileName());
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

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            // fileName follows the disposition, attachment without one
            "fileName, q3.pdf, null, attachment; filename=\"q3.pdf\"",
            "fileName, q3.pdf, INLINE, inline; filename=\"q3.pdf\"",
            "fileName, q3.pdf, ATTACHMENT, attachment; filename=\"q3.pdf\"",
            "fileName, null, INLINE, null", "fileName, null, null, null",
            "fileName, '', null, null", "fileName, '   ', INLINE, null",
            "fileName, '\u0000\r\n', null, null",
            "fileName, 'a\r\n.txt', INLINE, inline; filename=\"a.txt\"",
            // built-in handlers marked inline default to inline
            "fileNameInlineByDefault, q3.pdf, null, inline; filename=\"q3.pdf\"",
            "fileNameInlineByDefault, q3.pdf, ATTACHMENT, attachment; filename=\"q3.pdf\"",
            "fileNameAttachmentByDefault, q3.pdf, null, attachment; filename=\"q3.pdf\"",
            "fileNameAttachmentByDefault, q3.pdf, INLINE, inline; filename=\"q3.pdf\"",
            // explicit choices of the handler win over the disposition
            "attachment, q3.pdf, INLINE, attachment; filename=\"q3.pdf\"",
            "attachment, null, null, attachment",
            "setFileName, q3.pdf, INLINE, attachment; filename=\"q3.pdf\"",
            "inline, q3.pdf, ATTACHMENT, inline; filename=\"q3.pdf\"",
            "inlineNoName, null, ATTACHMENT, inline" })
    void dispositionInRequest_headerType(String method, String fileName,
            DownloadDisposition disposition, String expectedHeader) {
        when(request.getAttribute(
                StreamRequestHandler.DISPOSITION_REQUEST_ATTRIBUTE))
                .thenReturn(disposition);
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        switch (method) {
        case "fileName" -> downloadEvent.fileName(fileName);
        case "fileNameInlineByDefault" ->
            downloadEvent.fileName(fileName, true);
        case "fileNameAttachmentByDefault" ->
            downloadEvent.fileName(fileName, false);
        case "attachment" -> downloadEvent.attachment(fileName);
        case "setFileName" -> downloadEvent.setFileName(fileName);
        case "inline" -> downloadEvent.inline(fileName);
        case "inlineNoName" -> downloadEvent.inline();
        default -> throw new IllegalArgumentException(method);
        }
        if (expectedHeader == null) {
            verify(response, times(0)).setHeader(eq("Content-Disposition"),
                    anyString());
        } else {
            verify(response).setHeader("Content-Disposition", expectedHeader);
        }
    }

    @Test
    void dispositionInRequest_headerAlreadySet_doesNotOverrideHeader() {
        when(request.getAttribute(
                StreamRequestHandler.DISPOSITION_REQUEST_ATTRIBUTE))
                .thenReturn(DownloadDisposition.INLINE);
        when(response.containsHeader("Content-Disposition")).thenReturn(true);
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.fileName("q3.pdf");
        verify(response, times(0)).setHeader(eq("Content-Disposition"),
                anyString());
    }

    static Stream<Arguments> callSequences() {
        return Stream.of(Arguments.of("inline then fileName",
                (Consumer<DownloadEvent>) e -> {
                    e.inline();
                    e.fileName("q3.pdf");
                }, "inline; filename=\"q3.pdf\""), Arguments.of(
                        "fileName then inline", (Consumer<DownloadEvent>) e -> {
                            e.fileName("q3.pdf");
                            e.inline();
                        }, "inline; filename=\"q3.pdf\""),
                Arguments.of("attachment without name keeps name",
                        (Consumer<DownloadEvent>) e -> {
                            e.fileName("q3.pdf");
                            e.attachment(null);
                        }, "attachment; filename=\"q3.pdf\""),
                Arguments.of("inline with blank name keeps name",
                        (Consumer<DownloadEvent>) e -> {
                            e.fileName("q3.pdf");
                            e.inline("  ");
                        }, "inline; filename=\"q3.pdf\""),
                Arguments.of("built-in default keeps handler name",
                        (Consumer<DownloadEvent>) e -> {
                            e.fileName("custom.pdf");
                            e.fileName("default.bin", true);
                        }, "inline; filename=\"custom.pdf\""),
                Arguments.of("built-in default keeps handler type",
                        (Consumer<DownloadEvent>) e -> {
                            e.attachment("x.pdf");
                            e.fileName("default.bin", true);
                        }, "attachment; filename=\"x.pdf\""),
                Arguments.of("built-in name with handler inline",
                        (Consumer<DownloadEvent>) e -> {
                            e.inline();
                            e.fileName("default.bin", false);
                        }, "inline; filename=\"default.bin\""));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("callSequences")
    void callSequence_lastHeaderCombinesNameAndType(String description,
            Consumer<DownloadEvent> calls, String expectedHeader) {
        trackHeaderOnResponse();
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        calls.accept(downloadEvent);
        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response, atLeastOnce()).setHeader(eq("Content-Disposition"),
                header.capture());
        assertEquals(expectedHeader, header.getValue());
    }

    @Test
    void fileName_sameNameTwice_headerWrittenOnce() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.fileName("q3.pdf");
        downloadEvent.fileName("q3.pdf");
        verify(response, times(1)).setHeader(eq("Content-Disposition"),
                anyString());
    }

    // makes containsHeader report a header once it has been set, like a real
    // response
    private void trackHeaderOnResponse() {
        AtomicBoolean set = new AtomicBoolean();
        doAnswer(invocation -> {
            set.set(true);
            return null;
        }).when(response).setHeader(eq("Content-Disposition"), anyString());
        when(response.containsHeader("Content-Disposition"))
                .thenAnswer(invocation -> set.get());
    }
}
