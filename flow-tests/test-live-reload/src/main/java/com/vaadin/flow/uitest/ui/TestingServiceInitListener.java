/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class TestingServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        // just set a fake backend to trigger live-reload client-side
        BrowserLiveReloadAccess liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccess.class);
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
