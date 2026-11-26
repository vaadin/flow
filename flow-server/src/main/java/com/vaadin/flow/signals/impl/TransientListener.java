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
package com.vaadin.flow.signals.impl;

import java.io.Serializable;

/**
 * A listener that is expected to only be invoked the next time some event
 * occurs but not for subsequent events. The listener can optionally request
 * that it retained also for the following event.
 * 
 * @since 25.1
 */
@FunctionalInterface
public interface TransientListener extends Serializable {
    /**
     * Invoked when the next event occurs. The return value indicates whether
     * the listener should be retained.
     *
     * @param immediate
     *            <code>true</code> if the listener is invoked immediately when
     *            it is added, <code>false</code> if the event occurred after
     *            the listener was added
     *
     * @return <code>true</code> to invoke the listener also for the next event,
     *         <code>false</code> to stop invoking the listener
     */
    boolean invoke(boolean immediate);

    /**
     * Runs the given task in a context where the current transaction doesn't
     * hide changes that were submitted to a signal tree after the transaction
     * started reading from it. A transaction with repeatable reads keeps
     * returning the values it first read, so a change made by someone else in
     * the meantime only becomes visible once the transaction no longer applies.
     * A usage uses this to add the listener again from such a context so that
     * the listener learns about that change.
     * <p>
     * The default implementation doesn't run the task and returns
     * <code>false</code>. The usage then only invokes the listener for changes
     * that happen after the listener was added. A listener that wraps another
     * listener should delegate to the wrapped listener so that the wrapped
     * listener's context is used.
     *
     * @param task
     *            the task to run, not <code>null</code>
     * @return <code>true</code> if the task has been run or will be run,
     *         <code>false</code> if this listener has no context for running it
     */
    default boolean runWithFreshReads(Runnable task) {
        return false;
    }
}
