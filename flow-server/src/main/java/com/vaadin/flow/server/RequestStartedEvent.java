/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.EventObject;
import java.util.Optional;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when the framework starts handling a request, on the request thread and after
 * the {@link VaadinRequestInterceptor#requestStart request interceptors} have
 * run.
 * <p>
 * A message that a client sends through a push connection is reported as a
 * request of its own, without a response.
 * <p>
 * Every started event is followed by a {@link RequestEndedEvent} for the same
 * request on the same thread, so a listener may keep timing state in a
 * {@link ThreadLocal}. Requests are handled concurrently, so listeners must
 * expect several requests to be in flight on several threads at once.
 */
public class RequestStartedEvent extends EventObject {

    private final transient VaadinRequest request;
    private final transient VaadinResponse response;

    /**
     * Creates a new event.
     *
     * @param service
     *            the service handling the request, not {@code null}
     * @param request
     *            the request, not {@code null}
     * @param response
     *            the response, or {@code null} for a push message
     */
    public RequestStartedEvent(VaadinService service, VaadinRequest request,
            VaadinResponse response) {
        super(service);
        this.request = request;
        this.response = response;
    }

    /**
     * Gets the service handling the request.
     *
     * @return the service, not {@code null}
     */
    public VaadinService getService() {
        return (VaadinService) getSource();
    }

    /**
     * Gets the request.
     *
     * @return the request, not {@code null}
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Gets the response.
     *
     * @return the response, or an empty optional for a push message
     */
    public Optional<VaadinResponse> getResponse() {
        return Optional.ofNullable(response);
    }
}
