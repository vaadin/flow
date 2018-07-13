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
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

import com.vaadin.flow.server.VaadinServlet;

public class Activator implements BundleActivator {

    public static class MyServlet extends VaadinServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }

        @Override
        public ServletContext getServletContext() {
            return WebAppServletContextGrabber.getWebAppServetContext();
        }
    }

    @Override
    public void start(BundleContext ctx) throws Exception {
        ServletHandler handler = new ServletHandler();

        handler.addServletWithMapping(MyServlet.class, "/*");
        SessionHandler sessionHandler = new SessionHandler();
        handler.setHandler(sessionHandler);

        ServletContextHandler contextHandler = new ServletContextHandler();

        contextHandler.setServletHandler(handler);

        Dictionary<String, ?> props = new Hashtable();

        ctx.registerService(HttpService.class,
                new JettyHttpServiceFactory(handler), props);
        ctx.registerService(ContextHandler.class.getName(), contextHandler,
                props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}