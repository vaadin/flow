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
package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import com.vaadin.flow.server.communication.IndexHtmlRequestListener;

/**
 * Event fired to {@link VaadinServiceInitListener} when a {@link VaadinService}
 * is being initialized.
 * <p>
 * This event can also be used to add {@link RequestHandler}s that will be used
 * by the {@code VaadinService} for handling all requests.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServiceInitEvent extends EventObject {

    private List<RequestHandler> addedRequestHandlers = new ArrayList<>();
    private List<IndexHtmlRequestListener> addedIndexHtmlRequestListeners = new ArrayList<>();
    private List<DependencyFilter> addedDependencyFilters = new ArrayList<>();
    private List<VaadinRequestInterceptor> addedVaadinRequestInterceptors = new ArrayList<>();
    private Executor executor;

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
     * Adds a new request interceptor that will be used by this service.
     *
     * @param vaadinRequestInterceptor
     *            the request interceptor to add, not <code>null</code>
     */
    public void addVaadinRequestInterceptor(
            VaadinRequestInterceptor vaadinRequestInterceptor) {
        Objects.requireNonNull(vaadinRequestInterceptor,
                "Request Interceptor cannot be null");

        addedVaadinRequestInterceptors.add(vaadinRequestInterceptor);
    }

    /**
     * Sets the {@link Executor} to be used by Vaadin for running asynchronous
     * tasks.
     * <p>
     * The application can also benefit from this executor to submit its own
     * asynchronous tasks.
     * <p>
     * The developer is responsible for managing the executor's lifecycle, for
     * example, by registering a {@link VaadinService} destroy listener to shut
     * it down.
     * <p>
     * A {@literal null} value can be given to switch back to the Vaadin default
     * executor.
     *
     * @param executor
     *            the executor to set.
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
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

    /**
     * Gets a stream of all Vaadin request interceptors that have been added for
     * the service.
     *
     * @return the stream of added request interceptors
     */
    public Stream<VaadinRequestInterceptor> getAddedVaadinRequestInterceptor() {
        return addedVaadinRequestInterceptors.stream();
    }

    /**
     * Gets the optional {@link Executor} that is currently set to be used by
     * Vaadin for running asynchronous tasks.
     *
     * @return an {@link Optional} containing the {@link Executor}, or an empty
     *         {@link Optional} if no executor is set.
     */
    public Optional<Executor> getExecutor() {
        return Optional.ofNullable(executor);
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

}
