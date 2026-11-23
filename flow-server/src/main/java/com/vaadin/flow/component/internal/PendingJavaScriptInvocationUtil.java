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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.communication.PushConnection;

/**
 * Keeps track of how many JavaScript invocations have been scheduled for a UI
 * without being sent to the browser, and logs a warning when there are too many
 * of them.
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
     * The number of undelivered invocations for one UI that triggers the
     * warning. A number this high means that something is wrong regardless of
     * what the application does, so it is not made configurable.
     */
    static final int WARNING_THRESHOLD = 1000;

    private static final int MAX_LOGGED_EXPRESSION_LENGTH = 120;

    private PendingJavaScriptInvocationUtil() {
        // Only static helpers
    }

    /**
     * Records that the given invocation is waiting to be sent.
     * <p>
     * The count is kept per UI rather than per state node, since a running
     * application has far more state nodes than UIs and even a single field on
     * every node adds up. Nothing is logged here, since a request that
     * schedules many invocations sends them all in its response. Only
     * invocations still waiting when the session lock is acquired again are
     * reported, see {@link #checkUndeliveredInvocations(UIInternals)}.
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

        internals.addUndeliveredJsInvocations(1);
        internals.undeliveredJsInvocationCounted(invocation);
        return internals;
    }

    /**
     * Logs a warning if the UI of the given internals has an alarming number of
     * undelivered invocations. Called when the session lock is acquired, when
     * any invocation still counted was scheduled during an earlier hold of the
     * lock and has not been sent since.
     *
     * @param internals
     *            the internals of the UI to check, not <code>null</code>
     */
    static void checkUndeliveredInvocations(UIInternals internals) {
        int count = internals.addUndeliveredJsInvocations(0);
        if (count >= WARNING_THRESHOLD
                && internals.markUndeliveredJsInvocationsWarningLogged()) {
            Logger logger = getLogger();
            if (logger.isWarnEnabled()) {
                logger.warn(buildWarningMessage(
                        internals.findNewestUndeliveredJsInvocation(), count));
            }
        }
    }

    private static UIInternals findInternals(StateNode owner) {
        UI ui = getUI(owner);
        return ui == null ? null : ui.getInternals();
    }

    // Non-private for testing purposes
    static String buildWarningMessage(PendingJavaScriptInvocation newest,
            int count) {
        return String.format(
                "%d JavaScript invocations scheduled for this UI have not been sent to the browser yet. "
                        + "%s"
                        + "Undelivered invocations are kept in memory until they are sent, so a growing number of them eventually causes an OutOfMemoryError. "
                        + "If only the latest value is relevant for the client, for example a progress value or the current time, do not schedule a new invocation for every update: "
                        + "either keep the PendingJavaScriptResult returned by executeJs and call cancelExecution() on it before scheduling the next one, "
                        + "or set an element property instead (element.setProperty(...)), since only the last value of a property is sent. "
                        + "A background task can also compare UI.getLastUpdateSentTimestamp() against the current time to stop scheduling updates while the client is not receiving them. "
                        + "This is logged once per UI. Turn off the logger %s to silence the warning.",
                count, describeNewest(newest),
                PendingJavaScriptInvocationUtil.class.getName());
    }

    private static String describeNewest(PendingJavaScriptInvocation newest) {
        if (newest == null) {
            return "";
        }
        StateNode owner = newest.getOwner();
        String state = describeState(owner);
        return String.format(
                "The newest one still waiting was scheduled for %s with the expression %s. %s",
                owner.describe(),
                describeExpression(newest.getInvocation().getExpression()),
                state.isEmpty() ? "" : (state + " "));
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
                    "There is no open push connection for the UI (push mode %s), so nothing is delivered until the browser sends a request of its own, for example when polling is enabled or when the user interacts with the application. "
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
