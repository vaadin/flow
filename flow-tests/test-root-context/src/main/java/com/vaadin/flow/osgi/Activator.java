/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.osgi;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.StaticFileHandler;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.uitest.servlet.Es6UrlViewTestServlet;
import com.vaadin.flow.uitest.servlet.ProductionModeTimingDataViewTestServlet;
import com.vaadin.flow.uitest.servlet.ProductionModeViewTestServlet;
import com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet;
import com.vaadin.flow.uitest.servlet.RouterTestServlet;
import com.vaadin.flow.uitest.servlet.ViewTestServlet;
import com.vaadin.flow.uitest.ui.LogoutWithNotificationServlet;

public class Activator implements BundleActivator {

    private ServiceTracker<HttpService, HttpService> httpTracker;

    private static class ItStaticFileServer extends StaticFileServer {

        private final VaadinServletService servletService;

        private ItStaticFileServer(VaadinServletService servletService) {
            super(servletService);
            this.servletService = servletService;
        }

        @Override
        protected URL getStaticResource(String path) {
            if (path.endsWith(".class")) {
                return null;
            }
            return servletService.getStaticResource(path);
        }

    }

    @VaadinServletConfiguration(productionMode = false)
    private static class FixedViewServlet extends ViewTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }

        @Override
        protected StaticFileHandler createStaticFileHandler(
                VaadinServletService servletService) {
            return new ItStaticFileServer(servletService);
        }
    }

    @VaadinServletConfiguration(productionMode = false)
    private static class FixedRouterServlet extends RouterTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }

        @Override
        protected StaticFileHandler createStaticFileHandler(
                VaadinServletService servletService) {
            return new ItStaticFileServer(servletService);
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

        @Override
        protected StaticFileHandler createStaticFileHandler(
                VaadinServletService servletService) {
            return new ItStaticFileServer(servletService);
        }
    }

    @VaadinServletConfiguration(productionMode = true)
    private static class FixedProductionModeTimingDataViewServlet
            extends ProductionModeTimingDataViewTestServlet {
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

            getService().setClassLoader(getClass().getClassLoader());
        }

        @Override
        protected StaticFileHandler createStaticFileHandler(
                VaadinServletService servletService) {
            return new ItStaticFileServer(servletService);
        }
    }

    private static class FixedRouterLayoutCustomScopeServlet
            extends RouterLayoutCustomScopeServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }

        @Override
        protected StaticFileHandler createStaticFileHandler(
                VaadinServletService servletService) {
            return new ItStaticFileServer(servletService);
        }
    }

    @VaadinServletConfiguration(productionMode = true)
    private static class FixedEs6UrlViewServlet extends Es6UrlViewTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
        }

        @Override
        protected StaticFileHandler createStaticFileHandler(
                VaadinServletService servletService) {
            return new ItStaticFileServer(servletService);
        }

    }

    @Override
    public void start(BundleContext context) throws Exception {
        httpTracker = new ServiceTracker<HttpService, HttpService>(context,
                HttpService.class.getName(), null) {
            @Override
            public void removedService(ServiceReference<HttpService> reference,
                    HttpService service) {
                // HTTP service is no longer available, unregister our
                // servlet...
                service.unregister("/view/*");
                service.unregister("/context/*");
                service.unregister("/frontend/*");
                service.unregister("/new-router-session/*");
                service.unregister("/view-production/*");
                service.unregister("/view-production-timing/*");
                service.unregister("/view-es6-url/*");
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public HttpService addingService(
                    ServiceReference<HttpService> reference) {
                // HTTP service is available, register our servlet...
                HttpService httpService = this.context.getService(reference);
                Dictionary dictionary = new Hashtable<>();
                try {
                    httpService.registerServlet("/view/*",
                            new FixedViewServlet(), dictionary, null);
                    httpService.registerServlet("/context/*",
                            new FixedViewServlet(), dictionary, null);
                    httpService.registerServlet("/frontend/*",
                            new FixedViewServlet(), dictionary, null);
                    httpService.registerServlet("/new-router-session/*",
                            new FixedRouterServlet(), dictionary, null);
                    httpService.registerServlet("/view-production/*",
                            new FixedProductionModeViewServlet(), dictionary,
                            null);
                    httpService.registerServlet("/view-production-timing/*",
                            new FixedProductionModeTimingDataViewServlet(),
                            dictionary, null);
                    httpService.registerServlet("/view-es6-url/*",
                            new FixedEs6UrlViewServlet(), dictionary, null);
                    httpService.registerServlet("/logout-with-notification/*",
                            new FixedLogoutWithNotificationServlet(),
                            dictionary, null);
                    httpService.registerServlet("/router-layout-custom-scope/*",
                            new FixedRouterLayoutCustomScopeServlet(),
                            dictionary, null);
                } catch (ServletException | NamespaceException exception) {
                    throw new RuntimeException(exception);
                }
                return httpService;
            }
        };
        // start tracking all HTTP services...
        httpTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // stop tracking all HTTP services...
        httpTracker.close();
    }
}
