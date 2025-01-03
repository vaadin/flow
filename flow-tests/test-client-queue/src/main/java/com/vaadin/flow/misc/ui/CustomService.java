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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.TranslationFileRequestHandler;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.UnsupportedBrowserHandler;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.communication.FaviconHandler;
import com.vaadin.flow.server.communication.HeartbeatHandler;
import com.vaadin.flow.server.communication.IndexHtmlRequestHandler;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;
import com.vaadin.flow.server.communication.PushRequestHandler;
import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.server.communication.SessionRequestHandler;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.communication.WebComponentBootstrapHandler;
import com.vaadin.flow.server.communication.WebComponentProvider;

public class CustomService extends VaadinServletService {

    public CustomService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        super(servlet, deploymentConfiguration);
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = new ArrayList<>();
        handlers.add(new FaviconHandler());
        handlers.add(new JavaScriptBootstrapHandler());
        handlers.add(new SessionRequestHandler());
        handlers.add(new HeartbeatHandler());

        handlers.add(new CustomUidlRequestHandler());

        handlers.add(new UnsupportedBrowserHandler());

        handlers.add(new StreamRequestHandler());

        handlers.add(new PwaHandler(() -> getPwaRegistry()));

        handlers.add(new TranslationFileRequestHandler(
                getInstantiator().getI18NProvider()));

        handlers.add(new WebComponentBootstrapHandler());
        handlers.add(new WebComponentProvider());

        Mode mode = getDeploymentConfiguration().getMode();
        if (mode == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD
                || mode == Mode.DEVELOPMENT_BUNDLE) {
            Optional<DevModeHandler> handlerManager = DevModeHandlerManager
                    .getDevModeHandler(this);
            if (handlerManager.isPresent()) {
                DevModeHandler devModeHandler = handlerManager.get();
                // WebComponentProvider handler should run before DevModeHandler
                // to avoid responding with html contents when dev bundle is
                // not ready (e.g. dev-mode-not-ready.html)
                handlers.stream().filter(WebComponentProvider.class::isInstance)
                        .findFirst().map(handlers::indexOf)
                        .ifPresentOrElse(idx -> {
                            handlers.add(idx, devModeHandler);
                        }, () -> handlers.add(devModeHandler));
            }
        }

        // PushRequestHandler should run before DevModeHandler to avoid
        // responding with html contents when dev mode server is not ready
        // (e.g. dev-mode-not-ready.html)
        if (isAtmosphereAvailable()) {
            try {
                handlers.add(new PushRequestHandler(this));
            } catch (ServiceException e) {
                // Atmosphere init failed. Push won't work but we don't throw a
                // service exception as we don't want to prevent non-push
                // applications from working
            }
        }

        handlers.add(0, new IndexHtmlRequestHandler());
        return handlers;
    }
}
