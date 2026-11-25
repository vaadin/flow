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
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ContentSecurityPolicyMode;
import com.vaadin.flow.server.ContentSecurityPolicyViolationException;
import com.vaadin.flow.server.VaadinService;

/**
 * Reports calls to APIs that send JavaScript to the browser as a string, which
 * the browser compiles with <code>new Function</code> and a strict Content
 * Security Policy without <code>unsafe-eval</code> blocks.
 * <p>
 * What happens depends on the {@link ContentSecurityPolicyMode} of the current
 * service: nothing in {@link ContentSecurityPolicyMode#OFF}, a warning logged
 * once per call site and detail in {@link ContentSecurityPolicyMode#WARN}, and
 * a {@link ContentSecurityPolicyViolationException} in
 * {@link ContentSecurityPolicyMode#STRICT}. Every string-based call is
 * reported, including the ones that Vaadin itself makes. Both the warning and
 * the exception carry a stack trace that starts at the API and leads to the
 * code that called it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.4
 */
public final class ContentSecurityPolicyViolationReporter {

    private static final String MESSAGE = "JavaScript is sent to the browser as a string, which the browser has to compile and a Content Security Policy without 'unsafe-eval' blocks. The stack trace leads from the API to the code that called it.";

    // Bounded by the number of call sites in the code, and of the details
    // reported from them, for each service
    private static final Map<VaadinService, Set<String>> warnedCalls = Collections
            .synchronizedMap(new WeakHashMap<>());

    private ContentSecurityPolicyViolationReporter() {
        // Only static helpers
    }

    /**
     * Reports a call to an API that sends JavaScript to the browser as a
     * string. Called by the API itself, which is where the stack trace of the
     * report starts.
     * <p>
     * A warning is logged once per call site. Use
     * {@link #reportStringJavaScript(String)} when the call site does not tell
     * what is reported, e.g. when a component reports from an action that runs
     * before the client response.
     *
     * @throws ContentSecurityPolicyViolationException
     *             in the {@link ContentSecurityPolicyMode#STRICT} mode
     */
    public static void reportStringJavaScript() {
        reportStringJavaScriptWithDetail(null);
    }

    /**
     * Reports a call to an API that sends JavaScript to the browser as a
     * string, with a detail that tells what is reported. Called by the API
     * itself, which is where the stack trace of the report starts.
     * <p>
     * The detail is added to the message, and a warning is logged once per call
     * site and detail. This identifies the report when the call site alone does
     * not, e.g. when a component reports from an action that runs before the
     * client response, where the stack does not reach the application code that
     * set the JavaScript. Use {@link #reportStringJavaScript()} when the call
     * site is enough.
     *
     * @param detail
     *            a short description of what is reported, e.g. the path of the
     *            option that has the JavaScript, not <code>null</code>
     * @throws ContentSecurityPolicyViolationException
     *             in the {@link ContentSecurityPolicyMode#STRICT} mode
     */
    public static void reportStringJavaScript(String detail) {
        Objects.requireNonNull(detail, "The detail must not be null");
        reportStringJavaScriptWithDetail(detail);
    }

    /**
     * Checks whether string JavaScript is reported for the current service,
     * i.e. whether its {@link ContentSecurityPolicyMode} is
     * {@link ContentSecurityPolicyMode#WARN} or
     * {@link ContentSecurityPolicyMode#STRICT}. A caller that does extra work
     * to collect what it reports, e.g. to find the detail for
     * {@link #reportStringJavaScript(String)}, can skip that work when this
     * returns <code>false</code>.
     *
     * @return <code>true</code> if reports are logged or thrown,
     *         <code>false</code> if they are ignored
     */
    public static boolean isEnabled() {
        return isEnabled(
                getContentSecurityPolicyMode(VaadinService.getCurrent()));
    }

    private static boolean isEnabled(ContentSecurityPolicyMode mode) {
        return (mode == ContentSecurityPolicyMode.WARN)
                || (mode == ContentSecurityPolicyMode.STRICT);
    }

    private static ContentSecurityPolicyMode getContentSecurityPolicyMode(
            VaadinService service) {
        // No service outside of a request or UI.access, e.g. for a component
        // built in a background thread before it is attached, and no
        // configuration for a service created with the no-argument
        // constructor: there is no mode to apply then
        DeploymentConfiguration configuration = (service == null) ? null
                : service.getDeploymentConfiguration();
        return (configuration == null) ? ContentSecurityPolicyMode.OFF
                : configuration.getContentSecurityPolicyMode();
    }

    private static void reportStringJavaScriptWithDetail(String detail) {
        VaadinService service = VaadinService.getCurrent();
        ContentSecurityPolicyMode mode = getContentSecurityPolicyMode(service);
        if (!isEnabled(mode)) {
            return;
        }

        String message = (detail == null) ? MESSAGE
                : (MESSAGE + " The JavaScript is in \"" + detail + "\".");
        ContentSecurityPolicyViolationException violation = new ContentSecurityPolicyViolationException(
                message);
        StackTraceElement[] stack = violation.getStackTrace();
        // Start the trace at the API rather than at the two methods of this
        // class
        violation.setStackTrace(Arrays.copyOfRange(stack, 2, stack.length));

        if (mode == ContentSecurityPolicyMode.STRICT) {
            throw violation;
        }
        String callSite = getCallSite(violation.getStackTrace());
        // Different details from one call site are different reports
        String key = (detail == null) ? callSite : (detail + '\n' + callSite);
        if (warnedCalls
                .computeIfAbsent(service, k -> ConcurrentHashMap.newKeySet())
                .add(key)) {
            LoggerFactory
                    .getLogger(ContentSecurityPolicyViolationReporter.class)
                    .warn(message, violation);
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
