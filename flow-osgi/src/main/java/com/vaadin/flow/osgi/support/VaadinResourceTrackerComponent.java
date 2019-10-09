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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
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
 * @since 1.2
 */
@Component(immediate = true)
public class VaadinResourceTrackerComponent {

    private HttpService httpService;

    private final Map<Long, Delegate> resourceToRegistration = Collections
            .synchronizedMap(new LinkedHashMap<>());

    private final Map<Long, List<ServiceRegistration<? extends OsgiVaadinStaticResource>>> contributorToRegistrations = Collections
            .synchronizedMap(new LinkedHashMap<>());

    private static final class Delegate implements HttpContext {
        private final String alias;
        private final String path;
        private final Bundle bundle;

        private final AtomicReference<HttpContext> context = new AtomicReference<HttpContext>();

        Delegate(String alias, String path, Bundle bundle) {
            this.alias = alias;
            this.path = path;
            this.bundle = bundle;
        }

        boolean init(HttpService service) {
            return context.compareAndSet(null,
                    service.createDefaultHttpContext());
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request,
                HttpServletResponse response) throws IOException {
            return context.get().handleSecurity(request, response);
        }

        @Override
        public URL getResource(String name) {
            return bundle.getResource(name);
        }

        @Override
        public String getMimeType(String name) {
            return context.get().getMimeType(name);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = OsgiVaadinStaticResource.class, policy = ReferencePolicy.DYNAMIC)
    void bindResource(ServiceReference<OsgiVaadinStaticResource> resourceRef)
            throws NamespaceException {
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

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = OsgiVaadinContributor.class, policy = ReferencePolicy.DYNAMIC)
    void bindContributor(
            ServiceReference<OsgiVaadinContributor> contributorRef) {
        Bundle bundle = contributorRef.getBundle();
        BundleContext context = bundle.getBundleContext();

        OsgiVaadinContributor contributor = context.getService(contributorRef);
        if (contributor == null) {
            return;
        }
        Long serviceId = (Long) contributorRef
                .getProperty(Constants.SERVICE_ID);
        List<OsgiVaadinStaticResource> contributions = contributor
                .getContributions();
        contributorToRegistrations.put(serviceId, contributions.stream()
                .map(contribution -> context.registerService(
                        OsgiVaadinStaticResource.class, contribution, null))
                .collect(Collectors.toList()));
    }

    void unbindContributor(
            ServiceReference<OsgiVaadinContributor> contributorRef) {
        Long serviceId = (Long) contributorRef
                .getProperty(Constants.SERVICE_ID);
        List<ServiceRegistration<? extends OsgiVaadinStaticResource>> registrations = contributorToRegistrations
                .get(serviceId);
        if (registrations != null) {
            registrations.forEach(ServiceRegistration::unregister);
        }
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
        synchronized (resourceToRegistration) {
            for (Delegate registration : resourceToRegistration.values()) {
                if (registration.init(httpService)) {
                    httpService.registerResources(registration.alias,
                            registration.path, registration);
                }
            }
        }
    }

    @Deactivate
    void deactivate() {
        synchronized (resourceToRegistration) {
            resourceToRegistration.values().stream()
                    .forEach(this::unregisterResource);
            resourceToRegistration.clear();
        }
        synchronized (contributorToRegistrations) {
            contributorToRegistrations.values()
                    .forEach(registrations -> registrations
                            .forEach(ServiceRegistration::unregister));
            contributorToRegistrations.clear();
        }
        httpService = null;
    }

    private void registerResource(OsgiVaadinStaticResource resource,
            Bundle bundle, Long serviceId) throws NamespaceException {
        Delegate registration = new Delegate(resource.getAlias(),
                resource.getPath(), bundle);
        resourceToRegistration.put(serviceId, registration);
        if (httpService != null) {
            registration.init(httpService);
            httpService.registerResources(registration.alias, registration.path,
                    registration);
        }
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
