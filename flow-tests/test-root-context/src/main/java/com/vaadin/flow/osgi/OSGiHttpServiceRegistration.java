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

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpService;
import org.slf4j.LoggerFactory;

/**
 * @author Vaadin Ltd
 *
 */
public class OSGiHttpServiceRegistration {

    static final String RESOURCES_SERVLET = "resourcesServlet";

    public static void registerHttpService(ServletContext ctx) {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");
            Bundle bundle = FrameworkUtil.getBundle(JettyOsgiInitializer.class);
            Dictionary<String, ?> props = new Hashtable();
            bundle.getBundleContext().registerService(HttpService.class,
                    new OsgiHttpServiceFactory(ctx), props);

            /*
             * The code below is a hack for jetty-osgi-boot to register
             * "/VAADIN/static/client" URI via HttpService.
             *
             * OsgiHttpService.registerResources method may not be used for
             * servlet registration because it's too late to do this with the
             * current approach.
             *
             * So the servlet is registered here assuming there will be only one
             * resource registration for "/VAADIN/static/client".
             *
             * We should use some another proper Web server implementation
             * inside OSGi which provides HttpService. In this case all this
             * package can be just removed completely.
             */
            StaticResourceServlet servlet = new StaticResourceServlet();

            ServletRegistration.Dynamic dynamic = ctx
                    .addServlet(RESOURCES_SERVLET, servlet);
            dynamic.addMapping("/VAADIN/static/client/*");
            ctx.setAttribute(RESOURCES_SERVLET, servlet);

        } catch (ClassNotFoundException exception) {
            LoggerFactory.getLogger(OSGiHttpServiceRegistration.class)
                    .debug(exception.getMessage(), exception);
        }
    }
}
