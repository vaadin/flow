/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;
import java.util.Properties;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Servlet created to test the environment when the property
 * {@link Constants#FRONTEND_URL_ES6} is set.
 *
 * @since 1.0
 */
@WebServlet(asyncSupported = true, urlPatterns = "/view-es6-url/*")
@VaadinServletConfiguration(productionMode = true)
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
