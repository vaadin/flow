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
 * in {@link CspMode#OFF}, a warning logged once per API and call site in
 * {@link CspMode#WARN}, and a {@link CspViolationException} in
 * {@link CspMode#STRICT}. Every string-based call is reported, including the
 * ones that Vaadin itself makes. The call site is the innermost stack frame of
 * an application class, see {@link StackTraceUtil#isApplicationClass}, followed
 * by the framework code it went through when it did not call the API itself.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class CspViolationReporter {

    // Bounded by the number of call sites in the code for each service
    private static final Map<VaadinService, Set<String>> warnedCalls = Collections
            .synchronizedMap(new WeakHashMap<>());

    private CspViolationReporter() {
        // Only static helpers
    }

    /**
     * Reports a call to an API that sends JavaScript to the browser as a
     * string, naming the API and the code that called it.
     *
     * @param api
     *            the signature of the API that was called, e.g.
     *            <code>Element.executeJs(String, Object...)</code>
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringJavaScript(String api) {
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

        String message = String.format(
                "%s called at %s sends JavaScript as a string, which the browser has to compile and a Content Security Policy without 'unsafe-eval' blocks.",
                api, findCallSite());

        if (mode == CspMode.STRICT) {
            throw new CspViolationException(message);
        }
        if (warnedCalls
                .computeIfAbsent(service, key -> ConcurrentHashMap.newKeySet())
                .add(message)) {
            LoggerFactory.getLogger(CspViolationReporter.class).warn(message);
        }
    }

    private static String findCallSite() {
        // This method, reportStringJavaScript and the API come first
        StackTraceElement[] stack = new Throwable().getStackTrace();
        String apiClass = stack[2].getClassName();
        for (int i = 3; i < stack.length; i++) {
            if (StackTraceUtil.isApplicationClass(stack[i].getClassName())) {
                StackTraceElement callee = stack[i - 1];
                if (callee.getClassName().equals(apiClass)) {
                    return format(stack[i]);
                }
                return format(stack[i]) + " via " + format(callee);
            }
        }
        return "an unknown location";
    }

    private static String format(StackTraceElement frame) {
        return frame.getClassName() + "." + frame.getMethodName() + "("
                + frame.getFileName() + ":" + frame.getLineNumber() + ")";
    }
}
