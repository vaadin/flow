/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus;

import java.io.IOException;
import java.util.Optional;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Servlet to create {@link QuarkusVaadinServletService}.
 *
 * An instance of this servlet is automatically registered if no other custom
 * VaadinServlet class with Servlet 3.0 annotations is present on classpath. A
 * subclass of this servlet can be to provide a customized
 * {@link QuarkusVaadinServletService} implementation, in which case
 * {@link #createServletService(DeploymentConfiguration)} must call
 * {@code service.init()}.
 */
public class QuarkusVaadinServlet extends VaadinServlet {

    @Inject
    BeanManager beanManager;

    private static final ThreadLocal<Optional<String>> SERVLET_NAME = new ThreadLocal<>();

    @Override
    protected VaadinServletService createServletService(
            final DeploymentConfiguration configuration)
            throws ServiceException {

        final QuarkusVaadinServletService service = new QuarkusVaadinServletService(
                this, configuration, this.beanManager);
        service.init();
        return service;
    }

    @Override
    public void init(final ServletConfig servletConfig)
            throws ServletException {
        SERVLET_NAME.set(Optional.of(servletConfig.getServletName()));
        try {
            super.init(servletConfig);
        } finally {
            SERVLET_NAME.set(Optional.empty());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        SERVLET_NAME.remove();
    }

    @Override
    protected void service(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException {
        SERVLET_NAME.set(Optional.of(getServletName()));
        try {
            super.service(request, response);
        } finally {
            SERVLET_NAME.set(Optional.empty());
        }
    }

    /**
     * Name of the Vaadin servlet for the current thread.
     * <p>
     * Until VaadinService appears in CurrentInstance, it have to be used to get
     * the servlet name.
     * <p>
     * This method is meant for internal use only.
     *
     * @return currently processing vaadin servlet name
     * @see VaadinServlet#getCurrent()
     */
    public static Optional<String> getCurrentServletName() {
        return SERVLET_NAME.get();
    }

}
