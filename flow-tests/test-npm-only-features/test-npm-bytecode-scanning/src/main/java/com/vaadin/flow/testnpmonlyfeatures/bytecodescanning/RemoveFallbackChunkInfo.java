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
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FallbackChunk;

/**
 * This is a hacky code since it removes {@link FallbackChunk} instance
 * <b><u>per request</u></b>.
 * <p>
 * Once being removed the {@link FallbackChunk} instance can'be restored for the
 * whole application. So one (arbitrary) request changes the behavior of the
 * whole application (singleton). This is generally a terrible way of doing
 * things but in <u>this specific test case</u> it doesn't cause issues
 * <b><u>currently</u></b> with the tests (only one in fact) that we have. The
 * situation might change in the future.
 * <p>
 * This hack works <u>only</u> if all tests uses "drop-fallback" query parameter
 * or don't use it.
 */
public class RemoveFallbackChunkInfo implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler(this::handleRequest);
    }

    boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) {
        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        HttpServletRequest httpRequest = servletRequest.getHttpServletRequest();
        String query = httpRequest.getQueryString();
        if ("drop-fallback".equals(query)) {
            // self check
            FallbackChunk chunk = session.getService().getContext()
                    .getAttribute(FallbackChunk.class);

            if (chunk == null) {
                throw new RuntimeException(
                        "Vaadin context has no fallback chunk data");
            }

            // remove fallback chunk data to that the chunk won't be loaded
            session.getService().getContext()
                    .removeAttribute(FallbackChunk.class);
        }
        return false;
    }

}
