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
package com.vaadin.flow.quarkus.it.pushdispatch;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.http.HttpServletResponse;
import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

/**
 * Custom Vaadin error route for the deadlock signal. RESTEasy Reactive throws
 * {@link BlockingNotAllowedException} whenever a synchronous client call is
 * made on a Vert.x event-loop thread. With {@code @Push(WEBSOCKET)} and the
 * Quarkus default {@code dispatch-to-worker=false}, Vaadin's in-app navigation
 * runs through the established Push websocket on the event loop and the
 * {@link SlowGreetingClient#slowGreeting()} call in
 * {@link PushBlockingAfterNavView}'s {@code afterNavigation} hook triggers this
 * exception. Catching it here gives the IT a deterministic UI signal to assert
 * against, and rendering the stack trace alongside makes the cause visible in a
 * screenshot (or to anyone hitting the route manually in dev mode).
 */
public class PushBlockingErrorView extends Div
        implements HasErrorParameter<BlockingNotAllowedException> {

    public static final String VIEW_ID = "push-blocking-error";
    public static final String ERROR_TEXT = "BLOCKED_ON_EVENT_LOOP";
    public static final String STACKTRACE_ID = "push-blocking-error-stacktrace";

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<BlockingNotAllowedException> parameter) {
        setId(VIEW_ID);
        setText(ERROR_TEXT);

        Pre stackTrace = new Pre(stackTraceOf(parameter.getException()));
        stackTrace.setId(STACKTRACE_ID);
        add(stackTrace);

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
