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
package com.vaadin.flow.misc.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlRequestHandler;

public class CustomUidlRequestHandler extends UidlRequestHandler {

    public static Set<VaadinSession> emptyResponse = new HashSet();

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (emptyResponse.contains(session)) {
            emptyResponse.remove(session);
            commitEmptyResponse(response);
            return true;
        }
        return super.synchronizedHandleRequest(session, request, response);
    }

    @Override
    public Optional<ResponseWriter> synchronizedHandleRequest(
            VaadinSession session, VaadinRequest request,
            VaadinResponse response, String requestBody)
            throws IOException, UnsupportedOperationException {

        if (emptyResponse.contains(session)) {
            emptyResponse.remove(session);
            return Optional.of(() -> commitEmptyResponse(response));
        }
        return super.synchronizedHandleRequest(session, request, response,
                requestBody);
    }

    private void commitEmptyResponse(VaadinResponse response)
            throws IOException {
        commitJsonResponse(response, "for(;;);[{}]");
    }
}
