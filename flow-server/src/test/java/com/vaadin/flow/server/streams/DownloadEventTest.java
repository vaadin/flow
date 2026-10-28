/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.EncodeUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.Assert.*;

public class DownloadEventTest {
    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private VaadinService service;

    @Before
    public void setUp() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        session = Mockito.mock(VaadinSession.class);
        service = Mockito.mock(VaadinService.class);
    }

    private static Map<String, String> asciiFileNames() {
        return Map.of("test.txt", "test.txt", "a.pdf\"; filename*=UTF-8''b.exe",
                "a.pdf\\\"; filename*=UTF-8''b.exe", "a\\b\r\n\t.txt",
                "a\\\\b.txt");
    }

    @Test
    public void setFileName_asciiFileName_setsContentDispositionFilenameQuotedToResponse() {
        asciiFileNames().forEach((fileName, quotedContent) -> {
            VaadinResponse response = Mockito.mock(VaadinResponse.class);
            DownloadEvent downloadEvent = new DownloadEvent(request, response,
                    session, null);
            downloadEvent.setFileName(fileName);
            Mockito.verify(response).setHeader("Content-Disposition",
                    "attachment; filename=\"" + quotedContent + "\"");
        });
    }

    @Test
    public void setFileName_nonEmptyFileName_setsContentDispositionEncodedFilenameQuotedToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "test üñîçødë.txt";
        downloadEvent.setFileName(fileName);
        Mockito.verify(response).setHeader("Content-Disposition", "attachment;"
                + " filename=\"" + EncodeUtil.rfc2047Encode(fileName) + "\";"
                + " filename*=UTF-8''" + EncodeUtil.rfc5987Encode(fileName));
    }

    @Test
    public void setFileName_nonAsciiFileNameWithControlCharacters_controlCharactersRemoved() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName("ä\r\n\u0085.txt");
        Mockito.verify(response).setHeader("Content-Disposition", "attachment;"
                + " filename=\"" + EncodeUtil.rfc2047Encode("ä.txt") + "\";"
                + " filename*=UTF-8''" + EncodeUtil.rfc5987Encode("ä.txt"));
        assertEquals("ä.txt", downloadEvent.getFileName());
    }

    @Test
    public void setFileName_emptyFileName_setsContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "";
        downloadEvent.setFileName(fileName);
        Mockito.verify(response).setHeader("Content-Disposition", "attachment");
    }

    @Test
    public void setFileName_onlyControlCharacters_setsContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName("\u0000\r\n");
        Mockito.verify(response).setHeader("Content-Disposition", "attachment");
    }

    @Test
    public void setFileName_nullFileName_doesNotSetContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        downloadEvent.setFileName(null);
        Mockito.verify(response, Mockito.times(0))
                .setHeader(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void setContentType_setsContentTypeToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String contentType = "application/pdf";
        downloadEvent.setContentType(contentType);
        Mockito.verify(response).setContentType(contentType);
    }

    @Test
    public void setContentLenght_setsContentLengthToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        int contentLength = 1024;
        downloadEvent.setContentLength(contentLength);
        Mockito.verify(response).setContentLengthLong(contentLength);
    }

    @Test
    public void setContentLenght_unknownLength_doesNotSetContentLengthToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        int contentLength = -1;
        downloadEvent.setContentLength(contentLength);
        Mockito.verify(response, Mockito.times(0))
                .setContentLengthLong(contentLength);
    }
}
