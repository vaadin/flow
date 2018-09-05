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
package com.vaadin.flow.server.communication;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles the favicon request explicitly and return 404 for it.
 * <p>
 * It allows to not produce the same content for the favicon as for any other
 * resource if servlet mapping is "/*".
 * 
 * @author Vaadin Ltd
 * @since 1.0
 */
public class FaviconHandler implements RequestHandler {

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        boolean isFavicon = httpRequest.getContextPath().isEmpty()
                && httpRequest.getServletPath().isEmpty()
                && "/favicon.ico".equals(httpRequest.getPathInfo());
        if (isFavicon) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return isFavicon;
    }

}
