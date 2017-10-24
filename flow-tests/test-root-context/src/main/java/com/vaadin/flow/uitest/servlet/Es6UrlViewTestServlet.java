/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.servlet;

import java.util.Properties;

import javax.servlet.annotation.WebServlet;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.shared.ApplicationConstants;

/**
 * Servlet created to test the environment when the property
 * {@link Constants#FRONTEND_URL_ES6} is set.
 */
@WebServlet(asyncSupported = true, urlPatterns = { "/view-es6-url/*" })
@VaadinServletConfiguration(productionMode = false, usingNewRouting = true)
public class Es6UrlViewTestServlet extends ViewTestServlet {

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {

        // Configure frontend:// as <context>/frontend/com/vaadin/flow/uitest/
        initParameters.setProperty(Constants.FRONTEND_URL_ES6,
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                        + "frontend/com/vaadin/flow/uitest/");

        return super.createDeploymentConfiguration(initParameters);
    }
}
