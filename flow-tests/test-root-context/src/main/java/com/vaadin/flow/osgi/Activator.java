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
package com.vaadin.flow.osgi;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet;
import com.vaadin.flow.uitest.servlet.RouterTestServlet;
import com.vaadin.flow.uitest.servlet.ViewTestServlet;
import com.vaadin.flow.uitest.ui.LogoutWithNotificationServlet;

@Component(immediate = true)
public class Activator {

    public static class OsgiResourceRegistration {

    }

    private static final class CustomContextHelper
            extends ServletContextHelper {

        private CustomContextHelper(Bundle bundle) {
            super(bundle);
        }
    }

    private static class CustomContextHelperFactory
            implements ServiceFactory<ServletContextHelper> {

        @Override
        public ServletContextHelper getService(Bundle bundle,
                ServiceRegistration<ServletContextHelper> registration) {
            return new CustomContextHelper(bundle);
        }

        @Override
        public void ungetService(Bundle bundle,
                ServiceRegistration<ServletContextHelper> registration,
                ServletContextHelper service) {
            // no op
        }

    }

    private static class FixedViewServlet extends ViewTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            if (getService() != null) {
                getService().setClassLoader(getClass().getClassLoader());
            }
        }
    }

    private static class FixedRouterServlet extends RouterTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            if (getService() != null) {
                getService().setClassLoader(getClass().getClassLoader());
            }
        }
    }

    private static class FixedLogoutWithNotificationServlet
            extends LogoutWithNotificationServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            if (getService() != null) {
                getService().setClassLoader(getClass().getClassLoader());
            }
        }
    }

    private static class FixedRouterLayoutCustomScopeServlet
            extends RouterLayoutCustomScopeServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            if (getService() != null) {
                getService().setClassLoader(getClass().getClassLoader());
            }
        }
    }

    @Activate
    void activate() throws NamespaceException {
        BundleContext context = FrameworkUtil.getBundle(Activator.class)
                .getBundleContext();

        context.registerService(Servlet.class, new FixedViewServlet(),
                createProperties("/view/*", false));

        context.registerService(Servlet.class, new FixedViewServlet(),
                createProperties("/context/*", false));

        context.registerService(Servlet.class, new FixedRouterServlet(),
                createProperties("/new-router-session/*", false));

        context.registerService(Servlet.class,
                new FixedLogoutWithNotificationServlet(),
                createProperties("/logout-with-notification/*", false));

        context.registerService(Servlet.class,
                new FixedRouterLayoutCustomScopeServlet(),
                createProperties("/router-layout-custom-scope/*", false));

        registerPlainJsResource(context);

        String contextName = registerCustomContext(context);
        registerCustomContextServlet(context, contextName);
    }

    private void registerPlainJsResource(BundleContext context) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN,
                "/plain-script.js");
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX,
                "/osgi-web-resources/plain-script.js");
        context.registerService(OsgiResourceRegistration.class,
                new OsgiResourceRegistration(), properties);
    }

    private String registerCustomContext(BundleContext context) {
        Dictionary<String, String> contextProps = new Hashtable<String, String>();
        String contextName = "test-context";
        contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
                contextName);
        contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
                "/custom-test-context");
        context.registerService(ServletContextHelper.class,
                new CustomContextHelperFactory(), contextProps);
        return contextName;
    }

    private void registerCustomContextServlet(BundleContext context,
            String contextName) {
        Hashtable<String, Object> properties = createProperties("/view/*",
                false);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "="
                        + contextName + ")");
        context.registerService(Servlet.class, new FixedViewServlet(),
                properties);
    }

    private Hashtable<String, Object> createProperties(String mapping,
            Boolean isProductionMode) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX
                        + InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                isProductionMode.toString());
        properties.put(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED,
                true);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                mapping);
        return properties;
    }

}
