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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.uitest.servlet.Es6UrlViewTestServlet;
import com.vaadin.flow.uitest.servlet.ProductionModeTimingDataViewTestServlet;
import com.vaadin.flow.uitest.servlet.ProductionModeViewTestServlet;
import com.vaadin.flow.uitest.servlet.RouterTestServlet;
import com.vaadin.flow.uitest.servlet.ViewTestServlet;
import com.vaadin.flow.uitest.servlet.WebJarsServlet;

public class Activator implements BundleActivator {

    private ServiceTracker<HttpService, HttpService> httpTracker;

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

    @VaadinServletConfiguration(productionMode = true)
    private static class FixedEs6UrlViewServlet extends Es6UrlViewTestServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            getService().setClassLoader(getClass().getClassLoader());
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
                dictionary.put("bowerMode", Boolean.TRUE.toString());
                try {
                    httpService.registerServlet("/view/*",
                            new FixedViewServlet(), dictionary, null);
                    httpService.registerServlet("/frontend/*",
                            new WebJarsServlet(), dictionary, null);
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
