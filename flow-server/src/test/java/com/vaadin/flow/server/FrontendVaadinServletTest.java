/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

public class FrontendVaadinServletTest {

    @Test
    public void doNotServeNonStaticResources()
            throws ServletException, IOException {
        FrontendVaadinServlet servlet = new FrontendVaadinServlet() {
            @Override
            protected boolean serveStaticOrWebJarRequest(
                    HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                return false;
            }
        };
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        servlet.service(request, response);

        Mockito.verify(response).sendError(
                Mockito.eq(HttpServletResponse.SC_NOT_FOUND),
                Mockito.anyString());
    }

    @Test
    public void serveNonStaticResources() throws ServletException, IOException {
        FrontendVaadinServlet servlet = new FrontendVaadinServlet() {
            @Override
            protected boolean serveStaticOrWebJarRequest(
                    HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                return true;
            }
        };
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        servlet.service(request, response);

        Mockito.verifyZeroInteractions(response);
        Mockito.verifyZeroInteractions(request);
    }

}
