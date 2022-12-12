/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Processes a session resynchronize request from the client.
 *
 * Used for reloading embedded WebComponents in case session has been expired.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.0
 */
public class SessionResyncRequestHandler extends WebComponentBootstrapHandler {

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request, RequestType.SESSION_RESYNC);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        Class<? extends UI> uiClass = getUIClass(request);
        BootstrapContext context = createAndInitUI(uiClass, request, response,
                session);

        JsonObject json = new UidlWriter().createUidl(context.getUI(), true,
                true);
        json.put(ApplicationConstants.UI_ID, context.getUI().getUIId());
        json.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID,
                context.getUI().getCsrfToken());
        String responseString = "for(;;);[" + JsonUtil.stringify(json) + "]";

        VaadinService service = request.getService();
        service.writeUncachedStringResponse(response,
                JsonConstants.JSON_CONTENT_TYPE, responseString);

        return true;
    }

}
