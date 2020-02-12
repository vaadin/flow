/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;

public class UidlRequestHandlerTest {

    private VaadinRequest request;
    private VaadinResponse response;
    private OutputStream outputStream;

    private UidlRequestHandler handler;

    @Before
    public void setup() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        handler = new UidlRequestHandler();
    }

    @Test
    public void writeSessionExpired() throws Exception {

        VaadinService service = new VaadinServletService(null,
                new DefaultDeploymentConfiguration(getClass(),
                        new Properties()));
        Mockito.when(request.getService()).thenReturn(service);

        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(RequestType.UIDL.getIdentifier());

        boolean result = handler.handleSessionExpired(request, response);
        Assert.assertTrue("Result should be true", result);

        String responseContent = CommunicationUtil
                .getStringWhenWriteBytesOffsetLength(outputStream);

        // response shouldn't contain async
        Assert.assertEquals("Invalid response",
                "for(;;);[{\"meta\":{\"sessionExpired\":true}}]",
                responseContent);
    }

    @Test
    public void writeSessionExpired_whenUINotFound() throws IOException {

        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.getService()).thenReturn(service);

        Mockito.when(service.findUI(request)).thenReturn(null);

        boolean result = handler.synchronizedHandleRequest(session, request,
                response);
        Assert.assertTrue("Result should be true", result);

        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // response shouldn't contain async
        Assert.assertEquals("Invalid response",
                "for(;;);[{\"meta\":{\"sessionExpired\":true}}]",
                responseContent);
    }

}
