package com.vaadin.flow.server.streams;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void setFileName_setsContentDispositionToResponse() {
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, null);
        String fileName = "test.txt";
        downloadEvent.setFileName(fileName);

        Mockito.verify(response).setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"");
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
}