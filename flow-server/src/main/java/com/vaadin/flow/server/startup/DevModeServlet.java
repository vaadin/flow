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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.vaadin.flow.server.DevModeHandler;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Serves js bundle files and polyfills during development mode.
 */
public class DevModeServlet extends HttpServlet {

    private DevModeHandler devmodeHandler;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        devmodeHandler = DevModeHandler.getDevModeHandler();

        super.init(servletConfig);
    }

    @Override
    protected void service(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        if (devmodeHandler != null && devmodeHandler.isDevModeRequest(request)) {
            devmodeHandler.serveDevModeRequest(request, response);

        } else {
            response.sendError(SC_NOT_FOUND);

            // Close request to avoid issues in CI and Chrome
            response.getOutputStream().close();
        }
    }

}
