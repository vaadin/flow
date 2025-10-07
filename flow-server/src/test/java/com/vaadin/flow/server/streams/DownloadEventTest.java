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

import java.io.IOException;

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

    @Test
    public void setFileName_nonEmptyFileName_setsContentDispositionFilenameQuotedToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "test.txt";
        downloadEvent.setFileName(fileName);
        Mockito.verify(response).setHeader("Content-Disposition",
                "attachment;" + " filename=\"" + fileName + "\"");
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
    public void setFileName_emptyFileName_setsContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "";
        downloadEvent.setFileName(fileName);
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