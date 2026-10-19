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
package com.vaadin.flow.quarkus.test.servlet;

import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.quarkus.QuarkusVaadinServlet;

/**
 * A user-defined Vaadin servlet that sets every {@code @WebServlet} attribute
 * the extension copies to the servlet registration.
 */
@WebServlet(name = CustomVaadinServlet.NAME, urlPatterns = "/custom/*", asyncSupported = true, loadOnStartup = 1, initParams = @WebInitParam(name = CustomVaadinServlet.PARAM_NAME, value = CustomVaadinServlet.PARAM_VALUE))
public class CustomVaadinServlet extends QuarkusVaadinServlet {

    static final String NAME = "my-servlet";
    static final String PARAM_NAME = "my.param";
    static final String PARAM_VALUE = "a1b2";

    static final AtomicReference<CustomVaadinServlet> initialized = new AtomicReference<>();

    @Override
    protected void servletInitialized() {
        initialized.set(this);
    }
}
