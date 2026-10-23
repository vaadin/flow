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
package com.vaadin.flow.server.communication;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.SessionlessRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;

/**
 * Handles the favicon request explicitly and return 404 for it.
 * <p>
 * It allows to not produce the same content for the favicon as for any other
 * resource if servlet mapping is "/*". The favicon request does not need a
 * session, so it is answered without looking one up or creating one.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class FaviconHandler implements SessionlessRequestHandler {

    @Override
    public boolean handleRequest(VaadinRequest request,
            VaadinResponse response) {
        boolean isFavicon = request instanceof VaadinServletRequest httpRequest
                && httpRequest.getContextPath().isEmpty()
                && httpRequest.getServletPath().isEmpty()
                && "/favicon.ico".equals(httpRequest.getPathInfo());
        if (isFavicon) {
            response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
        }
        return isFavicon;
    }

}
