/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.uitest.servlet.ProductionModeTimingDataViewTestServlet;
import com.vaadin.flow.uitest.servlet.ProductionModeViewTestServlet;
import com.vaadin.flow.uitest.servlet.RouterTestServlet;
import com.vaadin.flow.uitest.servlet.ViewTestServlet;

@Component(immediate = true)
public class Activator {

    @VaadinServletConfiguration(productionMode = false)
    private static class FixedViewServlet extends ViewTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }
    }

    @VaadinServletConfiguration(productionMode = false)
    private static class FixedRouterServlet extends RouterTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }
    }

    @VaadinServletConfiguration(productionMode = true)
    private static class FixedProductionModeViewServlet
            extends ProductionModeViewTestServlet {

        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }
    }

    @VaadinServletConfiguration(productionMode = true)
    private static class FixedProductionModeTimingDataViewServlet
            extends ProductionModeTimingDataViewTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }
    }

    @Activate
    void activate() throws NamespaceException {
        BundleContext context = FrameworkUtil.getBundle(Activator.class)
                .getBundleContext();

        context.registerService(Servlet.class, new FixedViewServlet(),
                createProperties("/view/*"));

        context.registerService(Servlet.class, new FixedViewServlet(),
                createProperties("/context/*"));

        context.registerService(Servlet.class, new FixedRouterServlet(),
                createProperties("/new-router-session/*"));

        context.registerService(Servlet.class,
                new FixedProductionModeViewServlet(),
                createProperties("/view-production/*"));

        context.registerService(Servlet.class,
                new FixedProductionModeTimingDataViewServlet(),
                createProperties("/view-production-timing/*"));

    }

    private Hashtable<String, Object> createProperties(String mapping) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED,
                true);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                mapping);
        return properties;
    }

}
