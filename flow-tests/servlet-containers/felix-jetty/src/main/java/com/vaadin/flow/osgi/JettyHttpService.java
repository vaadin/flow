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
package com.vaadin.flow.osgi;

import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class JettyHttpService implements HttpService {

    private final ServletHandler handler;
    private final Bundle bundle;

    public JettyHttpService(ServletHandler handler, Bundle bundle) {
        this.handler = handler;
        this.bundle = bundle;
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new HttpContextImpl(bundle);
    }

    @Override
    public void registerResources(String alias, String path,
            HttpContext context) throws NamespaceException {
        if (context == null) {
            context = createDefaultHttpContext();
        }
        ServletHolder holder = new ServletHolder(
                new StaticResourceServlet(path, context));
        handler.addServletWithMapping(holder, alias + "/*");
    }

    @Override
    public void registerServlet(String arg0, Servlet arg1, Dictionary arg2,
            HttpContext arg3) throws ServletException, NamespaceException {

    }

    @Override
    public void unregister(String arg0) {

    }

}
