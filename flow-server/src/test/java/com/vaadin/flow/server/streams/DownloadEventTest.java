package com.vaadin.flow.server.streams;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
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
        Element element = new Element("a");
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, element);
        String fileName = "test.txt";
        downloadEvent.setFileName(fileName);
        Mockito.verify(response).setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"");
        Assert.assertTrue(element.hasAttribute("download"));
    }

    @Test
    public void setFileName_emptyFileName_setsContentDispositionToResponse() {
        Element element = new Element("a");
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, element);
        String fileName = "";
        downloadEvent.setFileName(fileName);
        Mockito.verify(response).setHeader("Content-Disposition", "attachment");
        Assert.assertTrue(element.hasAttribute("download"));
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