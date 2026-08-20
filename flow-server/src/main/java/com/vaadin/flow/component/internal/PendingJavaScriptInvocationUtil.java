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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeUtil;
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

        int count = internals.addUndeliveredJsInvocations(1);
        if (count == WARNING_THRESHOLD
                && internals.markUndeliveredJsInvocationsWarningLogged()) {
            Logger logger = getLogger();
            if (logger.isWarnEnabled()) {
                logger.warn(buildWarningMessage(invocation, count));
            }
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Call site of the JavaScript invocation that triggered the warning above",
                        new Throwable("executeJs call site"));
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

    // Non-private for testing purposes
    static String buildWarningMessage(PendingJavaScriptInvocation invocation,
            int count) {
        StateNode owner = invocation.getOwner();

        return String.format(
                "%d JavaScript invocations scheduled for this UI have not been sent to the browser yet. "
                        + "The most recent one was scheduled for %s with the expression %s. %s "
                        + "Undelivered invocations are kept in memory until they are sent, so a growing number of them eventually causes an OutOfMemoryError. "
                        + "If only the latest value is relevant for the client, for example a progress value or the current time, do not schedule a new invocation for every update: "
                        + "either keep the PendingJavaScriptResult returned by executeJs and call cancelExecution() on it before scheduling the next one, "
                        + "or set an element property instead (element.setProperty(...)), since only the last value of a property is sent. "
                        + "A background task can also compare UI.getLastUpdateSentTimestamp() against the current time to stop scheduling updates while the client is not receiving them. "
                        + "This is logged once per UI. Enable debug logging for %s to also log the call site of the invocation, or turn that logger off to silence the warning.",
                count, describeOwner(owner),
                describeExpression(invocation.getInvocation().getExpression()),
                describeState(owner),
                PendingJavaScriptInvocationUtil.class.getName());
    }

    private static String describeOwner(StateNode owner) {
        String description = StateNodeUtil.describeTarget(owner);

        Component component = BasicElementStateProvider.get().supports(owner)
                ? Element.get(owner).getComponent().orElse(null)
                : null;
        if (component == null) {
            return description;
        }
        ComponentTracker.Location location = ComponentTracker
                .findCreate(component);
        if (location == null) {
            // Component tracking is only enabled in development mode
            return description;
        }
        return description + ", created at " + location.filename() + ":"
                + location.lineNumber();
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
