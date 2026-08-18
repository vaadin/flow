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
package com.vaadin.flow.component.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.communication.PushConnection;

/**
 * Keeps track of how many JavaScript invocations have been scheduled for a
 * state node without being sent to the browser, and logs a warning when the
 * number keeps growing.
 * <p>
 * Invocations are retained in memory until they are sent to the browser, so an
 * application that schedules invocations that are never delivered - typically
 * because the browser is not receiving updates, or because the owner is
 * detached or invisible - will eventually run out of memory.
 *
 * @see PendingJavaScriptInvocation
 */
final class PendingJavaScriptInvocationUtil {

    /**
     * The number of undelivered invocations for one owner that triggers the
     * first warning, unless configured with
     * {@link InitParameters#PENDING_JAVASCRIPT_INVOCATIONS_WARNING_THRESHOLD}.
     */
    static final int DEFAULT_WARNING_THRESHOLD = 1000;

    /**
     * Invocation counts are only inspected at multiples of this value to keep
     * the scheduling path cheap, which means that a configured threshold is in
     * practice rounded up to a multiple of it.
     */
    private static final int CHECK_GRANULARITY = 100;

    private static final int MAX_LOGGED_EXPRESSION_LENGTH = 120;

    private PendingJavaScriptInvocationUtil() {
        // Only static helpers
    }

    /**
     * Records that the given invocation has been scheduled, and logs a warning
     * if its UI has an alarming number of undelivered invocations.
     * <p>
     * The count is kept per UI rather than per state node, since a running
     * application has far more state nodes than UIs and even a single field on
     * every node adds up.
     *
     * @param invocation
     *            the scheduled invocation, not <code>null</code>
     * @return the UI internals the invocation was counted in, or
     *         <code>null</code> if no UI could be resolved for it
     */
    static UIInternals invocationScheduled(
            PendingJavaScriptInvocation invocation) {
        UIInternals internals = findInternals(invocation.getOwner());
        if (internals == null) {
            // Nothing to count in: the owner is detached and there is no
            // current UI, so the invocation is tracked once it reaches a UI
            return null;
        }

        int count = internals.incrementUndeliveredJsInvocations();
        if (count % CHECK_GRANULARITY == 0) {
            int threshold = getWarningThreshold();
            if (shouldWarn(count, threshold)) {
                Logger logger = getLogger();
                logger.warn(buildWarningMessage(invocation, count, threshold));
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Call site of the JavaScript invocation that triggered the warning above",
                            new Throwable("executeJs call site"));
                }
            }
        }
        return internals;
    }

    private static UIInternals findInternals(StateNode owner) {
        UI ui = getUI(owner);
        if (ui == null) {
            // An invocation scheduled for a detached owner is counted in the UI
            // that scheduled it, since that is the UI whose code keeps it alive
            ui = UI.getCurrent();
        }
        return ui == null ? null : ui.getInternals();
    }

    /**
     * Checks whether the given number of undelivered invocations should be
     * warned about. The first warning is logged when the threshold is reached
     * and repeated whenever the count grows tenfold, so that a leaking
     * application logs a handful of warnings instead of thousands.
     *
     * @param count
     *            the number of undelivered invocations
     * @param threshold
     *            the configured warning threshold, or a non-positive value if
     *            warnings are disabled
     * @return <code>true</code> if a warning should be logged
     */
    // Non-private for testing purposes
    static boolean shouldWarn(int count, int threshold) {
        if (threshold <= 0 || count < threshold || count % threshold != 0) {
            return false;
        }
        return isPowerOfTen(count / threshold);
    }

    private static boolean isPowerOfTen(int value) {
        int remaining = value;
        while (remaining > 1 && remaining % 10 == 0) {
            remaining /= 10;
        }
        return remaining == 1;
    }

    private static int getWarningThreshold() {
        VaadinService service = VaadinService.getCurrent();
        if (service == null) {
            return DEFAULT_WARNING_THRESHOLD;
        }
        String configured = service.getDeploymentConfiguration()
                .getStringProperty(
                        InitParameters.PENDING_JAVASCRIPT_INVOCATIONS_WARNING_THRESHOLD,
                        null);
        if (configured == null) {
            return DEFAULT_WARNING_THRESHOLD;
        }
        try {
            return Integer.parseInt(configured.trim());
        } catch (NumberFormatException e) {
            getLogger().warn(
                    "Ignoring the value '{}' of the {} configuration property since it is not a number. Using the default of {}.",
                    configured,
                    InitParameters.PENDING_JAVASCRIPT_INVOCATIONS_WARNING_THRESHOLD,
                    DEFAULT_WARNING_THRESHOLD);
            return DEFAULT_WARNING_THRESHOLD;
        }
    }

    // Non-private for testing purposes
    static String buildWarningMessage(PendingJavaScriptInvocation invocation,
            int count, int threshold) {
        StateNode owner = invocation.getOwner();

        return String.format(
                "%d JavaScript invocations scheduled for this UI have not been sent to the browser yet. "
                        + "The most recent one was scheduled for %s with the expression %s. %s "
                        + "Undelivered invocations are kept in memory until they are sent, so a growing number of them eventually causes an OutOfMemoryError. "
                        + "If only the latest value is relevant for the client, for example a progress value or the current time, do not schedule a new invocation for every update: "
                        + "either keep the PendingJavaScriptResult returned by executeJs and call cancelExecution() on it before scheduling the next one, "
                        + "or set an element property instead (element.setProperty(...)), since only the last value of a property is sent. "
                        + "A background task can also compare UI.getLastUpdateSentTimestamp() against the current time to stop scheduling updates while the client is not receiving them. "
                        + "This warning repeats when the count grows tenfold. Enable debug logging for %s to log the call site, or set the %s configuration property to change the threshold of %d (0 disables the warning).",
                count, describeOwner(owner),
                describeExpression(invocation.getInvocation().getExpression()),
                describeState(owner),
                PendingJavaScriptInvocationUtil.class.getName(),
                InitParameters.PENDING_JAVASCRIPT_INVOCATIONS_WARNING_THRESHOLD,
                threshold);
    }

    private static String describeOwner(StateNode owner) {
        Component component = owner.hasFeature(ComponentMapping.class)
                ? ComponentMapping.getComponent(owner).orElse(null)
                : null;
        if (component == null) {
            return "state node " + owner.getId();
        }

        String description = component.getClass().getName() + " (state node "
                + owner.getId() + ")";
        ComponentTracker.Location location = ComponentTracker
                .findCreate(component);
        if (location != null) {
            return description + " created at " + location.filename() + ":"
                    + location.lineNumber();
        }
        return description;
    }

    private static String describeExpression(String expression) {
        String singleLine = expression.replaceAll("\\s+", " ").trim();
        if (singleLine.length() > MAX_LOGGED_EXPRESSION_LENGTH) {
            singleLine = singleLine.substring(0, MAX_LOGGED_EXPRESSION_LENGTH)
                    + "...";
        }
        return "'" + singleLine + "'";
    }

    private static String describeState(StateNode owner) {
        if (!owner.isAttached()) {
            return "The owner is not attached to a UI, and invocations for a detached owner are held until it is attached again, so check that server code is not updating discarded components.";
        }
        if (!owner.isVisible()) {
            return "The owner is currently invisible, and invocations for an invisible owner are retained until it becomes visible again.";
        }

        UI ui = getUI(owner);
        if (ui == null) {
            return "";
        }
        PushConnection pushConnection = ui.getInternals().getPushConnection();
        if (pushConnection == null || !pushConnection.isConnected()) {
            return String.format(
                    "There is no open push connection for the UI (push mode %s), so nothing can be delivered until the browser sends a request. "
                            + "A closed browser tab whose session has not expired yet, or a push connection that was never established, look exactly like this.",
                    ui.getPushConfiguration().getPushMode());
        }
        return "The UI has an open push connection, so the invocations are most likely scheduled faster than the browser can consume them.";
    }

    private static UI getUI(StateNode owner) {
        NodeOwner nodeOwner = owner.getOwner();
        if (nodeOwner instanceof StateTree stateTree) {
            return stateTree.getUI();
        }
        return null;
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(PendingJavaScriptInvocationUtil.class.getName());
    }
}
