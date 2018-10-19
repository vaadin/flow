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
package com.vaadin.flow.client.osgi;

import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.vaadin.flow.client.ClientResources;

/**
 * Bundle activator for the client-side module.
 * <p>
 * The activator registers {@link ClientResources} service to allow to access to
 * the resources in this bundle.
 *
 * @author Vaadin Ltd
 *
 */
public class Activator implements BundleActivator, ServiceListener {

    @Override
    public void start(BundleContext context) throws Exception {
        context.registerService(ClientResources.class,
                new OsgiClientResources(), new Hashtable<String, String>());
        registerClientResources(null);
        context.addServiceListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Does nothing since the framework will automatically unregister any
        // registered services.
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            registerClientResources(event.getServiceReference());
        }
    }

    private void registerClientResources(ServiceReference<?> serviceReference) {
        Bundle bundle = FrameworkUtil.getBundle(Activator.class);
        ServiceReference<HttpService> reference = bundle.getBundleContext()
                .getServiceReference(HttpService.class);
        if (reference != null && (serviceReference == null
                || serviceReference.equals(reference))) {
            HttpService service = bundle.getBundleContext()
                    .getService(reference);
            try {
                service.registerResources("/VAADIN/static/client",
                        "/META-INF/resources/VAADIN/static/client", null);
            } catch (NamespaceException e) {
                throw new IllegalStateException(e);
            }
        }

    }

}
