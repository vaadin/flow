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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Counts the JavaScript invocations that have been scheduled for a UI without
 * being sent to the browser, and logs a warning when they pile up.
 * <p>
 * Invocations are retained in memory until they are sent to the browser, so an
 * application that keeps scheduling invocations that are not delivered will
 * eventually run out of memory. Invocations only waiting for the response of
 * the current request or access are not counted against the application, so a
 * big view built in one go is not reported. What is reported is told apart by
 * {@link UndeliveredJsInvocations}.
 *
 * @see PendingJavaScriptInvocation
 */
final class PendingJavaScriptInvocationUtil {

    /**
     * The number of undelivered invocations for one UI that triggers a warning.
     * A number this high means that something is wrong regardless of what the
     * application does, so it is not made configurable.
     */
    static final int WARNING_THRESHOLD = 1000;

    private static final int MAX_LOGGED_EXPRESSION_LENGTH = 120;

    private static final String ADVICE = "Undelivered invocations are kept in memory until they are sent, so a growing number of them eventually causes an OutOfMemoryError. "
            + "If only the latest value is relevant for the client, for example a progress value or the current time, do not schedule a new invocation for every update: "
            + "either keep the PendingJavaScriptResult returned by executeJs and call cancelExecution() on it before scheduling the next one, "
            + "or set an element property instead (element.setProperty(...)), since only the last value of a property is sent.";

    /**
     * The ways a UI can get invocations to the browser without the user doing
     * anything.
     */
    enum Delivery {
        PUSH, MANUAL_PUSH, PUSH_NOT_CONNECTED, POLLING, NONE;

        static Delivery of(UI ui) {
            PushMode pushMode = ui.getPushConfiguration().getPushMode();
            if (pushMode.isEnabled()) {
                PushConnection connection = ui.getInternals()
                        .getPushConnection();
                if (connection == null || !connection.isConnected()) {
                    return PUSH_NOT_CONNECTED;
                }
                return pushMode == PushMode.MANUAL ? MANUAL_PUSH : PUSH;
            }
            return ui.getPollInterval() > 0 ? POLLING : NONE;
        }
    }

    private PendingJavaScriptInvocationUtil() {
        // Only static helpers
    }

    /**
     * Records that the given invocation is waiting to be sent.
     *
     * @param invocation
     *            the invocation waiting to be sent, not <code>null</code>
     * @return the UI internals the invocation was counted in, or
     *         <code>null</code> if there is no UI to count it in
     */
    static UIInternals invocationScheduled(
            PendingJavaScriptInvocation invocation) {
        UIInternals internals = findInternals(invocation.getOwner());
        if (internals == null || internals.getSession() == null) {
            // An invocation is counted while its owner is attached to an open
            // UI, which is when it is on its way to a client. There is no
            // count for it before that, and none either when closing a UI
            // fails to detach its nodes and leaves them attached without a
            // session, which is logged and carried on from. A count that only
            // backs a warning is not worth failing an application over, so the
            // invocation is counted in the UI it is attached to next
            return null;
        }
        internals.undeliveredJsInvocationScheduled(invocation);
        return internals;
    }

    private static UIInternals findInternals(StateNode owner) {
        UI ui = getUI(owner);
        return ui == null ? null : ui.getInternals();
    }

    static void warnInvisibleOwners(
            List<PendingJavaScriptInvocation> retained) {
        log(buildInvisibleOwnersMessage(retained));
    }

    static void warnNoDelivery(UIInternals internals, int waiting,
            PendingJavaScriptInvocation newest, Throwable newestCallSite) {
        log(buildNoDeliveryMessage(internals.getUI(), waiting, newest),
                newestCallSite);
    }

    static void warnDeliveryTooSlow(UIInternals internals, int waiting,
            int lockHolds, Delivery delivery,
            PendingJavaScriptInvocation newest, Throwable newestCallSite) {
        log(buildDeliveryTooSlowMessage(internals.getUI(), waiting, lockHolds,
                delivery, newest), newestCallSite);
    }

    /**
     * Records where an invocation is counted from, if debug logging is enabled
     * to show it in the warnings.
     *
     * @return the call site, or <code>null</code> if it is not recorded
     */
    static Throwable captureCallSite() {
        return getLogger().isDebugEnabled()
                ? new Throwable("executeJs call site")
                : null;
    }

    // Non-private for testing purposes
    static String buildInvisibleOwnersMessage(
            List<PendingJavaScriptInvocation> retained) {
        Map.Entry<StateNode, Long> top = retained.stream()
                .collect(Collectors.groupingBy(
                        PendingJavaScriptInvocation::getOwner,
                        Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue)).orElseThrow();
        PendingJavaScriptInvocation newest = retained.get(retained.size() - 1);
        return String.format(
                "%d JavaScript invocations for invisible components of this UI have not been sent to the browser. "
                        + "Invocations for an invisible component are kept until it becomes visible again or is detached. "
                        + "%d of them are for %s. The newest one was scheduled for %s with the expression %s. "
                        + "%s A component that is hidden for a long time can also skip its updates while it is not visible, and update itself when it is shown again. "
                        + "%s",
                retained.size(), top.getValue(), top.getKey().describe(),
                newest.getOwner().describe(),
                describeExpression(newest.getInvocation().getExpression()),
                ADVICE, onceNote());
    }

    // Non-private for testing purposes
    static String buildNoDeliveryMessage(UI ui, int waiting,
            PendingJavaScriptInvocation newest) {
        return String.format(
                "%d JavaScript invocations scheduled for this UI over separate holds of the session lock are waiting for the browser to send a request. "
                        + "The UI uses neither push (push mode %s) nor polling, so nothing is sent until the user interacts with the application. %s"
                        + "Changes made from a background thread with UI.access(...) need push or polling to reach the browser, and a closed browser tab whose session has not expired yet looks exactly like this. "
                        + "%s A background task can compare UI.getLastUpdateSentTimestamp() against the current time to stop scheduling updates while the client is not receiving them. "
                        + "%s",
                waiting, ui.getPushConfiguration().getPushMode(),
                describeNewest(newest), ADVICE, onceNote());
    }

    // Non-private for testing purposes
    static String buildDeliveryTooSlowMessage(UI ui, int waiting, int lockHolds,
            Delivery delivery, PendingJavaScriptInvocation newest) {
        return String.format(
                "%d JavaScript invocations scheduled for this UI over %d separate holds of the session lock have not been sent to the browser yet. "
                        + "%s %s"
                        + "%s A background task can compare UI.getLastUpdateSentTimestamp() against the current time to stop scheduling updates while the client is not receiving them. "
                        + "%s",
                waiting, lockHolds, describeDelivery(ui, delivery),
                describeNewest(newest), ADVICE, onceNote());
    }

    private static String describeDelivery(UI ui, Delivery delivery) {
        return switch (delivery) {
        case POLLING -> String.format(
                "The UI polls every %d ms, so invocations are being scheduled faster than polling sends them.",
                ui.getPollInterval());
        case MANUAL_PUSH ->
            "The UI uses manual push, so they are sent only when UI.push() is called, which is not often enough for how fast they are scheduled.";
        case PUSH_NOT_CONNECTED -> String.format(
                "The UI uses push (push mode %s) but has no open push connection, so they are sent only once the browser connects again. "
                        + "A closed browser tab whose session has not expired yet looks exactly like this.",
                ui.getPushConfiguration().getPushMode());
        case PUSH ->
            "The UI has an open push connection, so they are most likely scheduled faster than the browser can receive them.";
        case NONE ->
            "The UI uses neither push nor polling, so nothing is sent until the user interacts with the application.";
        };
    }

    private static String describeNewest(PendingJavaScriptInvocation newest) {
        if (newest == null) {
            return "";
        }
        return String.format(
                "The newest one still waiting was scheduled for %s with the expression %s. ",
                newest.getOwner().describe(),
                describeExpression(newest.getInvocation().getExpression()));
    }

    private static String onceNote() {
        return String.format(
                "This is logged once per UI. Enable debug logging for %s to also log where the newest invocation was scheduled from, or turn that logger off to silence the warning.",
                PendingJavaScriptInvocationUtil.class.getName());
    }

    private static String describeExpression(String expression) {
        String singleLine = expression.replaceAll("\\s+", " ").trim();
        if (singleLine.length() > MAX_LOGGED_EXPRESSION_LENGTH) {
            singleLine = singleLine.substring(0, MAX_LOGGED_EXPRESSION_LENGTH)
                    + "...";
        }
        return "'" + singleLine + "'";
    }

    private static UI getUI(StateNode owner) {
        NodeOwner nodeOwner = owner.getOwner();
        if (nodeOwner instanceof StateTree stateTree) {
            return stateTree.getUI();
        }
        return null;
    }

    private static void log(String message) {
        getLogger().warn(message);
    }

    private static void log(String message, Throwable newestCallSite) {
        log(message);
        if (newestCallSite != null) {
            getLogger().debug(
                    "Call site of the newest JavaScript invocation in the warning above",
                    newestCallSite);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(PendingJavaScriptInvocationUtil.class.getName());
    }
}
