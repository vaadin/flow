package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Random;

public class WebBrowserTest {

    private Random random = new Random();
    @Mock
    private VaadinRequest vaadinRequest;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getViewPort() {
        WebBrowser webBrowser = new WebBrowser();
        webBrowser.updateRequestDetails(vaadinRequest);
        Assert.assertEquals("Should return default value if v-ch is not available",
                webBrowser.getBrowserWindowHeight(), -1);
        Assert.assertEquals("Should return default value if v-cw is not available",
                webBrowser.getBrowserWindowWidth(), -1);

        int clientHeight = random.nextInt();
        int clientWidth = random.nextInt();
        Mockito.when(vaadinRequest.getParameter("v-sw")).thenReturn("100");
        Mockito.when(vaadinRequest.getParameter("v-ch"))
                .thenReturn(String.valueOf(clientHeight));
        Mockito.when(vaadinRequest.getParameter("v-cw"))
                .thenReturn(String.valueOf(clientWidth));
        webBrowser.updateRequestDetails(vaadinRequest);
        Assert.assertEquals("Should return v-ch value",
                webBrowser.getBrowserWindowHeight(), clientHeight);
        Assert.assertEquals("Should return v-cw value",
                webBrowser.getBrowserWindowWidth(), clientWidth);

        Mockito.when(vaadinRequest.getParameter("v-sw")).thenReturn("100");
        Mockito.when(vaadinRequest.getParameter("v-ch")).thenReturn("false");
        Mockito.when(vaadinRequest.getParameter("v-cw")).thenReturn("false");
        webBrowser.updateRequestDetails(vaadinRequest);
        Assert.assertEquals("Should return default value if v-ch is not valid int",
                webBrowser.getBrowserWindowHeight(), -1);
        Assert.assertEquals("Should return default value if v-cw is not valid int",
                webBrowser.getBrowserWindowWidth(), -1);
    }
}
