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
package com.vaadin.flow.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.CspMode;
import com.vaadin.flow.server.CspViolationException;
import com.vaadin.flow.server.VaadinService;

/**
 * Reports calls to APIs that send JavaScript to the browser as a string, which
 * the browser compiles with <code>new Function</code> and a strict Content
 * Security Policy without <code>unsafe-eval</code> blocks.
 * <p>
 * What happens depends on the {@link CspMode} of the current service: nothing
 * in {@link CspMode#OFF}, a warning logged once per call site in
 * {@link CspMode#WARN}, and a {@link CspViolationException} in
 * {@link CspMode#STRICT}. Every string-based call is reported, including the
 * ones that Vaadin itself makes. Both the warning and the exception carry a
 * stack trace that starts at the API and leads to the code that called it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class CspViolationReporter {

    private static final String MESSAGE = "JavaScript is sent to the browser as a string, which the browser has to compile and a Content Security Policy without 'unsafe-eval' blocks. The stack trace leads from the API to the code that called it.";

    // Bounded by the number of call sites in the code for each service
    private static final Map<VaadinService, Set<String>> warnedCalls = Collections
            .synchronizedMap(new WeakHashMap<>());

    private CspViolationReporter() {
        // Only static helpers
    }

    /**
     * Reports a call to an API that sends JavaScript to the browser as a
     * string. Called by the API itself, which is where the stack trace of the
     * report starts.
     *
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringJavaScript() {
        // No service outside of a request or UI.access, e.g. for a component
        // built in a background thread before it is attached, and no
        // configuration for a service created with the no-argument
        // constructor: there is no mode to apply then
        VaadinService service = VaadinService.getCurrent();
        DeploymentConfiguration configuration = service == null ? null
                : service.getDeploymentConfiguration();
        CspMode mode = configuration == null ? CspMode.OFF
                : configuration.getCspMode();
        if (mode != CspMode.WARN && mode != CspMode.STRICT) {
            return;
        }

        CspViolationException violation = new CspViolationException(MESSAGE);
        StackTraceElement[] stack = violation.getStackTrace();
        // Start the trace at the API rather than at this method
        violation.setStackTrace(Arrays.copyOfRange(stack, 1, stack.length));

        if (mode == CspMode.STRICT) {
            throw violation;
        }
        if (warnedCalls
                .computeIfAbsent(service, key -> ConcurrentHashMap.newKeySet())
                .add(getCallSite(violation.getStackTrace()))) {
            LoggerFactory.getLogger(CspViolationReporter.class).warn(MESSAGE,
                    violation);
        }
    }

    /**
     * Identifies a call site by the frames from the API up to and including the
     * first frame of application code, so that the same application line
     * reaching the API through different framework code counts as different
     * call sites.
     */
    private static String getCallSite(StackTraceElement[] stack) {
        StringBuilder callSite = new StringBuilder();
        for (StackTraceElement frame : stack) {
            callSite.append(frame).append('\n');
            if (StackTraceUtil.isApplicationClass(frame.getClassName())) {
                break;
            }
        }
        return callSite.toString();
    }
}
