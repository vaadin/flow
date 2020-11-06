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
package com.vaadin.flow.osgi.support;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
 * @author Stefan Bischof
 * @since 1.2
 */
@Component(immediate = true)
public class VaadinResourceTrackerComponent {

    private static final class Delegate implements HttpContext {
        private final String alias;
        private final Bundle bundle;
        private final HttpContext httpContext;

        private final String path;

        Delegate(String alias, String path, Bundle bundle,
                HttpContext httpContext) {
            this.alias = alias;
            this.path = path;
            this.bundle = bundle;
            this.httpContext = httpContext;
        }

        @Override
        public String getMimeType(String name) {
            return httpContext.getMimeType(name);
        }

        @Override
        public URL getResource(String name) {
            return bundle.getResource(name);
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request,
                HttpServletResponse response) throws IOException {
            return httpContext.handleSecurity(request, response);
        }

    }

    private final BundleContext bundleContext;

    private final Map<OsgiVaadinContributor, List<ServiceRegistration<? extends OsgiVaadinStaticResource>>> contributorToRegistrations = Collections
            .synchronizedMap(new LinkedHashMap<>());

    private final HttpService httpService;

    @Activate
    public VaadinResourceTrackerComponent(BundleContext bundleContext,
            @Reference(cardinality = ReferenceCardinality.MANDATORY) HttpService httpService) {
        this.httpService = httpService;
        this.bundleContext = bundleContext;
    }

    // OSGi - declarative services
    void unbindContributor(OsgiVaadinContributor contributor) {
        List<ServiceRegistration<? extends OsgiVaadinStaticResource>> registrations = contributorToRegistrations
                .remove(contributor);
        if (registrations != null) {
            registrations.forEach(ServiceRegistration::unregister);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = OsgiVaadinContributor.class, policy = ReferencePolicy.DYNAMIC)
    void bindContributor(OsgiVaadinContributor contributor) {
        List<OsgiVaadinStaticResource> contributions = contributor
                .getContributions();

        contributorToRegistrations.put(contributor, contributions.stream()
                .map(contribution -> bundleContext.registerService(
                        OsgiVaadinStaticResource.class, contribution, null))
                .collect(Collectors.toList()));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = OsgiVaadinStaticResource.class, policy = ReferencePolicy.DYNAMIC)
    void bindResource(OsgiVaadinStaticResource resource)
            throws NamespaceException {
        Bundle bundle = FrameworkUtil.getBundle(resource.getClass());

        Delegate registration = new Delegate(resource.getAlias(),
                resource.getPath(), bundle,
                httpService.createDefaultHttpContext());

        httpService.registerResources(registration.alias, registration.path,
                registration);
    }

    // OSGi - declarative services
    void unbindResource(OsgiVaadinStaticResource resource) {
        httpService.unregister(resource.getAlias());
    }
}
