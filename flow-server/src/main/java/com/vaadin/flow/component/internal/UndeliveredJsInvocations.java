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

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;

/**
 * The bookkeeping of the JavaScript invocations of one UI that have been
 * scheduled without being sent to the browser, used to tell apart the ways in
 * which they can pile up.
 * <p>
 * Invocations that are only waiting for the response of the current request or
 * access are not a problem, however many of them there are. Only these are:
 * <ul>
 * <li>invocations for invisible owners, which stay in the queue when everything
 * else is sent, checked whenever the queue is sent,
 * <li>invocations that keep being scheduled for a UI that has no way of getting
 * them to the browser without the user interacting with the application, i.e.
 * neither push nor polling,
 * <li>invocations that keep being scheduled faster than push or polling sends
 * them.
 * </ul>
 * The last two are checked when the session lock is released, and only count as
 * piling up once they have grown over separate holds of the lock since the
 * queue was last sent. A single hold that schedules many invocations, such as
 * an upload or a background task building a big view, is sent in one go by the
 * next request, push or poll. Each of them is logged at most once per UI, with
 * a message of its own.
 * <p>
 * Kept in a single object per UI rather than on the individual state nodes,
 * since a running application has far more state nodes than UIs. Serialized
 * with the UI together with the invocations that make up the count, each of
 * which references the UI internals until it stops being counted.
 *
 * @see PendingJavaScriptInvocationUtil
 */
final class UndeliveredJsInvocations implements Serializable {

    /**
     * The ways in which undelivered invocations can pile up, each of which is
     * logged once per UI.
     */
    enum PileUp {
        INVISIBLE_OWNERS, NO_DELIVERY, DELIVERY_TOO_SLOW
    }

    private int count;

    /**
     * How many of the counted invocations stayed in the queue for invisible
     * owners when it was last sent. An invocation that stops being counted
     * while retained is not subtracted from this, so in between sends the
     * number of other waiting invocations can only be underestimated.
     */
    private int retainedForInvisibleOwners;

    private int countAtLastLockRelease;

    /**
     * The number of session lock holds since the queue was last sent during
     * which more invocations were scheduled for the UI than were sent or
     * discarded.
     */
    private int growingLockHoldsSinceSent;

    /**
     * The newest counted invocation, to name in a warning if it is still
     * waiting by then. Forgotten when the queue is sent, so that a sent
     * invocation is not kept in memory until the next one is scheduled.
     */
    private PendingJavaScriptInvocation newest;

    /**
     * Where {@link #newest} was counted from, only recorded while debug logging
     * is enabled for {@link PendingJavaScriptInvocationUtil}.
     */
    private transient Throwable newestCallSite;

    private final EnumSet<PileUp> logged = EnumSet.noneOf(PileUp.class);

    /**
     * Changes the number of undelivered invocations by the given delta.
     *
     * @param delta
     *            the number of invocations to add, negative for invocations
     *            that are no longer waiting to be sent
     * @return the number of undelivered invocations after the change, never
     *         negative
     */
    int add(int delta) {
        // Only an invocation that was counted uncounts itself, so the count
        // is not expected to go negative. Clamped rather than asserted since
        // a slip in the bookkeeping behind a warning is not worth failing an
        // application over
        count = Math.max(0, count + delta);
        return count;
    }

    /**
     * Counts the given invocation, which is now waiting to be sent.
     *
     * @param invocation
     *            the invocation, not <code>null</code>
     */
    void scheduled(PendingJavaScriptInvocation invocation) {
        add(1);
        newest = invocation;
        newestCallSite = PendingJavaScriptInvocationUtil.captureCallSite();
    }

    /**
     * Called when the queue of the UI has been sent to the browser, with the
     * invocations that stay in it because their owners are invisible.
     *
     * @param retained
     *            the invocations left in the queue, in the order they were
     *            queued, not <code>null</code>
     */
    void queueSent(List<PendingJavaScriptInvocation> retained) {
        retainedForInvisibleOwners = retained.size();
        countAtLastLockRelease = count;
        growingLockHoldsSinceSent = 0;
        if (newest != null && !isWaiting(newest)) {
            newest = null;
            newestCallSite = null;
        }
        if (retained.size() >= PendingJavaScriptInvocationUtil.WARNING_THRESHOLD
                && markLogged(PileUp.INVISIBLE_OWNERS)) {
            PendingJavaScriptInvocationUtil.warnInvisibleOwners(retained);
        }
    }

    /**
     * Called when the session lock is about to be released, after everything
     * that was going to be sent to the browser while holding it has been sent.
     * Whatever is still waiting then waits for a later request, push or poll.
     *
     * @param internals
     *            the internals of the UI, not <code>null</code>
     */
    void lockReleasing(UIInternals internals) {
        if (count > countAtLastLockRelease) {
            growingLockHoldsSinceSent++;
        }
        countAtLastLockRelease = count;

        int waiting = count - retainedForInvisibleOwners;
        if (waiting < PendingJavaScriptInvocationUtil.WARNING_THRESHOLD
                || growingLockHoldsSinceSent < 2) {
            return;
        }
        PendingJavaScriptInvocationUtil.Delivery delivery = PendingJavaScriptInvocationUtil.Delivery
                .of(internals.getUI());
        boolean newestWaiting = newest != null && isWaiting(newest);
        PendingJavaScriptInvocation named = newestWaiting ? newest : null;
        Throwable callSite = newestWaiting ? newestCallSite : null;
        if (delivery == PendingJavaScriptInvocationUtil.Delivery.NONE) {
            if (markLogged(PileUp.NO_DELIVERY)) {
                PendingJavaScriptInvocationUtil.warnNoDelivery(internals,
                        waiting, named, callSite);
            }
        } else if (markLogged(PileUp.DELIVERY_TOO_SLOW)) {
            PendingJavaScriptInvocationUtil.warnDeliveryTooSlow(internals,
                    waiting, growingLockHoldsSinceSent, delivery, named,
                    callSite);
        }
    }

    // Non-private for testing purposes
    boolean isLogged(PileUp pileUp) {
        return logged.contains(pileUp);
    }

    private boolean markLogged(PileUp pileUp) {
        return logged.add(pileUp);
    }

    private static boolean isWaiting(PendingJavaScriptInvocation invocation) {
        return !invocation.isSentToBrowser() && !invocation.isCanceled();
    }
}
