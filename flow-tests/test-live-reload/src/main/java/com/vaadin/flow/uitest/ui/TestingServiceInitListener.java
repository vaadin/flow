/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class TestingServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        // just set a fake backend to trigger live-reload client-side
        BrowserLiveReloadAccessor liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccessor.class);
        BrowserLiveReload browserLiveReload = liveReloadAccess
                .getLiveReload(VaadinService.getCurrent());
        browserLiveReload.setBackend(BrowserLiveReload.Backend.HOTSWAP_AGENT);

        event.addRequestHandler(
                (RequestHandler) (session, request, response) -> {
                    if ("/reset_frontend".equals(request.getPathInfo())) {
                        FrontendLiveReloadView
                                .resetFrontendFile(session.getService());
                        return true;
                    } else if ("/update_frontend"
                            .equals(request.getPathInfo())) {
                        String code = IOUtils.toString(request.getInputStream(),
                                StandardCharsets.UTF_8.name());
                        FrontendLiveReloadView.replaceFrontendFile(
                                session.getService(), code);
                        return true;
                    } else {
                        return false;
                    }
                });
    }

}
