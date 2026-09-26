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

import java.time.Duration;
import java.util.EventObject;
import java.util.Optional;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when the framework has handled a request and written the response, on the
 * request thread and before the {@link VaadinRequestInterceptor#requestEnd
 * request interceptors} run.
 * <p>
 * Besides the request, the event tells which request handler handled the
 * request and which exception, if any, made handling it fail. The exception has
 * already been passed to the session {@link ErrorHandler} at that point.
 *
 * @see RequestStartedEvent
 */
public class RequestEndedEvent extends EventObject {

    private final transient VaadinRequest request;
    private final transient VaadinResponse response;
    private final transient VaadinSession session;
    private final transient RequestHandler handler;
    private final transient Exception failure;
    private final Duration duration;

    /**
     * Creates a new event.
     *
     * @param service
     *            the service that handled the request, not {@code null}
     * @param request
     *            the request, not {@code null}
     * @param response
     *            the response, or {@code null} for a push message
     * @param session
     *            the session used during the request, or {@code null} if the
     *            request did not use a session
     * @param handler
     *            the request handler that handled the request, or {@code null}
     *            if no request handler handled it
     * @param failure
     *            the exception that made handling the request fail, or
     *            {@code null} if handling it did not fail
     * @param duration
     *            the time from the start of handling the request until now, not
     *            {@code null}
     */
    public RequestEndedEvent(VaadinService service, VaadinRequest request,
            VaadinResponse response, VaadinSession session,
            RequestHandler handler, Exception failure, Duration duration) {
        super(service);
        this.request = request;
        this.response = response;
        this.session = session;
        this.handler = handler;
        this.failure = failure;
        this.duration = duration;
    }

    /**
     * Gets the service that handled the request.
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

    /**
     * Gets the session used during the request.
     *
     * @return the session, or an empty optional if the request did not use a
     *         session
     */
    public Optional<VaadinSession> getSession() {
        return Optional.ofNullable(session);
    }

    /**
     * Gets the request handler that handled the request. The type of the
     * handler tells what kind of request it was, for example
     * {@link com.vaadin.flow.server.communication.UidlRequestHandler} for a
     * client-to-server message.
     * <p>
     * A push message is not handled by a request handler, and neither is a
     * request that failed before a handler accepted it.
     *
     * @return the request handler, or an empty optional if no request handler
     *         handled the request
     */
    public Optional<RequestHandler> getHandler() {
        return Optional.ofNullable(handler);
    }

    /**
     * Gets the exception that made handling the request fail.
     *
     * @return the exception, or an empty optional if handling the request did
     *         not fail
     */
    public Optional<Exception> getFailure() {
        return Optional.ofNullable(failure);
    }

    /**
     * Gets the time it took to handle the request, measured from just before
     * the {@link RequestStartedEvent} was fired.
     *
     * @return the duration, not {@code null}
     */
    public Duration getDuration() {
        return duration;
    }
}
