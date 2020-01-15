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
package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.server.communication.IndexHtmlRequestListener;

/**
 * Event fired to {@link VaadinServiceInitListener} when a {@link VaadinService}
 * is being initialized.
 * <p>
 * This event can also be used to add {@link RequestHandler}s that will be used
 * by the {@code VaadinService} for handling all requests.
 * <p>
 * {@link BootstrapListener}s can also be registered, that are used to modify
 * the initial HTML of the application.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServiceInitEvent extends EventObject {

    private List<RequestHandler> addedRequestHandlers = new ArrayList<>();
    private List<BootstrapListener> addedBootstrapListeners = new ArrayList<>();
    private List<IndexHtmlRequestListener> addedIndexHtmlRequestListeners = new ArrayList<>();
    private List<DependencyFilter> addedDependencyFilters = new ArrayList<>();

    /**
     * Creates a new service init event for a given {@link VaadinService} and
     * the {@link RequestHandler} that will be used by the service.
     *
     * @param service
     *            the Vaadin service of this request
     */
    public ServiceInitEvent(VaadinService service) {
        super(service);
    }

    /**
     * Adds a new request handler that will be used by this service. The added
     * handler will be run before any of the framework's own request handlers,
     * but the ordering relative to other custom handlers is not guaranteed.
     *
     * @param requestHandler
     *            the request handler to add, not <code>null</code>
     */
    public void addRequestHandler(RequestHandler requestHandler) {
        Objects.requireNonNull(requestHandler,
                "Request handler cannot be null");

        addedRequestHandlers.add(requestHandler);
    }

    /**
     * Adds a new bootstrap listener that will be used by this service. The
     * ordering of multiple added bootstrap listeners is not guaranteed.
     *
     * @param bootstrapListener
     *            the bootstrap listener to add, not <code>null</code>
     * @deprecated Since 3.0, this API is deprecated in favor of
     *             {@link ServiceInitEvent#addIndexHtmlRequestListener} when
     *             using client-side bootstrapping
     */
    @Deprecated
    public void addBootstrapListener(BootstrapListener bootstrapListener) {
        if (getSource().getDeploymentConfiguration().isClientSideMode()) {
            throw new IllegalStateException(""
                    + "The BootstrapListener API is not supported in the "
                    + "client-side mode. Please use the IndexHtmlRequestListener "
                    + "API instead, or disable the client-side mode with"
                    + "-Dvaadin.clientSideMode=false to keep compatibility "
                    + "with V14.");
        }

        Objects.requireNonNull(bootstrapListener,
                "Bootstrap listener cannot be null");

        addedBootstrapListeners.add(bootstrapListener);
    }

    /**
     * Adds a new Index HTML request listener that will be used by this service.
     * The ordering of multiple added bootstrap listeners is not guaranteed.
     * 
     * @param indexHtmlRequestListener
     *            the Index HTML request listener to be added.
     */
    public void addIndexHtmlRequestListener(
            IndexHtmlRequestListener indexHtmlRequestListener) {
        Objects.requireNonNull(indexHtmlRequestListener,
                "Index HTML request listener cannot be null");
        addedIndexHtmlRequestListeners.add(indexHtmlRequestListener);
    }

    /**
     * Adds a new dependency filter that will be used by this service.
     *
     * @param dependencyFilter
     *            the dependency filter to add, not <code>null</code>
     */
    public void addDependencyFilter(DependencyFilter dependencyFilter) {
        Objects.requireNonNull(dependencyFilter,
                "Dependency filter cannot be null");

        addedDependencyFilters.add(dependencyFilter);
    }

    /**
     * Gets a stream of all custom request handlers that have been added for the
     * service.
     *
     * @return the stream of added request handlers
     */
    public Stream<RequestHandler> getAddedRequestHandlers() {
        return addedRequestHandlers.stream();
    }

    /**
     * Gets a stream of all bootstrap listeners that have been added for the
     * service.
     *
     * @return the stream of added bootstrap listeners
     * @deprecated Since 3.0, this API is deprecated in favor of
     *             {@link ServiceInitEvent#getAddedIndexHtmlRequestListeners()}
     *             when using client-side bootstrapping
     */
    @Deprecated
    public Stream<BootstrapListener> getAddedBootstrapListeners() {
        return addedBootstrapListeners.stream();
    }

    /**
     * Gets a stream of all Index HTML request listeners that have been added
     * for the service.
     *
     * @return the stream of added Index HTML request listeners
     */
    public Stream<IndexHtmlRequestListener> getAddedIndexHtmlRequestListeners() {
        return addedIndexHtmlRequestListeners.stream();
    }

    /**
     * Gets a stream of all dependency filters that have been added for the
     * service.
     *
     * @return the stream of added dependency filters
     */
    public Stream<DependencyFilter> getAddedDependencyFilters() {
        return addedDependencyFilters.stream();
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

}
