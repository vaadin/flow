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
package com.vaadin.flow.uitest.ui.push;

import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;

import org.atmosphere.cpr.ApplicationConfig;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Servlet with short push idle timeouts, so that a test can let the push
 * connection idle past them in seconds instead of the default five minutes of
 * the websocket idle timeout.
 */
@WebServlet(urlPatterns = "/idle-push/*", asyncSupported = true, initParams = {
        @WebInitParam(name = ApplicationConfig.WEBSOCKET_IDLETIME, value = ""
                + IdlePushChannelServlet.IDLE_TIMEOUT_MS),
        @WebInitParam(name = InitParameters.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING, value = ""
                + IdlePushChannelServlet.IDLE_TIMEOUT_MS) })
public class IdlePushChannelServlet extends VaadinServlet {

    /**
     * Idle time after which the server closes a websocket push connection and
     * resumes a suspended long polling request.
     */
    public static final int IDLE_TIMEOUT_MS = 3000;
}
