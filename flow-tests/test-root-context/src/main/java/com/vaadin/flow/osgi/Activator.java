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

import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * @author Vaadin Ltd
 *
 */
public class Activator implements BundleActivator, ServiceListener {

    @Override
    public void start(BundleContext context) throws Exception {
        context.addServiceListener(this);
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            registerResources(event.getServiceReference());
        }
    }

    private void registerResources(ServiceReference<?> serviceReference) {
        Bundle bundle = FrameworkUtil.getBundle(Activator.class);
        ServiceReference<HttpService> reference = bundle.getBundleContext()
                .getServiceReference(HttpService.class);
        if (reference != null && (serviceReference == null
                || serviceReference.equals(reference))) {
            HttpService service = bundle.getBundleContext()
                    .getService(reference);

            String resoursesPrefix = "/META-INF/resources";

            try {
                registerResource(service, resoursesPrefix + "/frontend",
                        resoursesPrefix.length(), bundle);
                registerResource(service, resoursesPrefix + "/frontend-es6",
                        resoursesPrefix.length(), bundle);
                service.registerResources("/include.js",
                        resoursesPrefix + "/include.js", null);
            } catch (NamespaceException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    private void registerResource(HttpService service, String folder,
            int prefixLength, Bundle bundle) throws NamespaceException {
        Enumeration<URL> entries = bundle.findEntries(folder, "*", false);
        while (entries.hasMoreElements()) {
            URL nextElement = entries.nextElement();
            String file = nextElement.getFile();
            if (file.endsWith("/")) {
                file = file.substring(0, file.length() - 1);
            }
            String path = file.substring(prefixLength);
            service.registerResources(path, file, null);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
