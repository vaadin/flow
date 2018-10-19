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
package com.vaadin.flow.osgi.support;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * Tracks {@link OsgiVaadinStaticResource} registration and uses
 * {@link HttpService} to register them.
 *
 * @author Vaadin Ltd
 *
 */
@Component(immediate = true)
public class VaadinResourceTrackerComponent {

    private HttpService httpService;

    private final Map<Long, Delegate> resourceToRegistration = Collections
            .synchronizedMap(new LinkedHashMap<>());

    private static final class Delegate implements HttpContext {
        private final String alias;
        private final String path;
        private final Bundle bundle;

        private volatile HttpContext context;

        public Delegate(String alias, String path, Bundle bundle) {
            this.alias = alias;
            this.path = path;
            this.bundle = bundle;
        }

        public void init(HttpService service) {
            context = service.createDefaultHttpContext();
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request,
                HttpServletResponse response) throws IOException {
            return context.handleSecurity(request, response);
        }

        @Override
        public URL getResource(String name) {
            return bundle.getResource(name);
        }

        @Override
        public String getMimeType(String name) {
            return context.getMimeType(name);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = OsgiVaadinStaticResource.class, policy = ReferencePolicy.DYNAMIC)
    void bindResource(ServiceReference<OsgiVaadinStaticResource> resourceRef) {
        Bundle bundle = resourceRef.getBundle();
        BundleContext context = bundle.getBundleContext();

        OsgiVaadinStaticResource resource = context.getService(resourceRef);
        if (resource == null) {
            return;
        }

        Long serviceId = (Long) resourceRef.getProperty(Constants.SERVICE_ID);
        try {
            registerResource(resource, bundle, serviceId);
        } finally {
            context.ungetService(resourceRef);
        }
    }

    void unbindResource(
            ServiceReference<OsgiVaadinStaticResource> resourceRef) {
        Long serviceId = (Long) resourceRef.getProperty(Constants.SERVICE_ID);
        unregisterResource(serviceId);
    }

    @Reference
    void setHttpService(HttpService service) {
        this.httpService = service;
    }

    void unsetHttpService(HttpService service) {
        this.httpService = null;
    }

    @Activate
    void activate() throws NamespaceException {
        for (Delegate registration : resourceToRegistration.values()) {
            registration.init(httpService);
            httpService.registerResources(registration.alias, registration.path,
                    registration);
        }
    }

    @Deactivate
    void deactivate() {
        for (final Delegate registration : resourceToRegistration.values()) {
            unregisterResource(registration);
        }
        resourceToRegistration.clear();
        httpService = null;
    }

    private void registerResource(OsgiVaadinStaticResource resource,
            Bundle bundle, Long serviceId) {
        resourceToRegistration.put(serviceId,
                new Delegate(resource.getAlias(), resource.getPath(), bundle));
    }

    private void unregisterResource(Long serviceId) {
        Delegate registration = resourceToRegistration.remove(serviceId);
        unregisterResource(registration);
    }

    private void unregisterResource(Delegate registration) {
        if (registration != null && httpService != null) {
            httpService.unregister(registration.alias);
        }
    }
}