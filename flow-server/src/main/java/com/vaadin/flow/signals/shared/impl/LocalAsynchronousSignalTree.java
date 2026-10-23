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
package com.vaadin.flow.signals.shared.impl;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalEnvironment;

/**
 * An asynchronous signal tree for single-JVM use that dispatches
 * {@link #confirm(List)} using the default effect dispatcher from
 * {@link SignalEnvironment}. This makes the behavior consistent with clustered
 * implementations where confirmation happens asynchronously. Submitted commands
 * are confirmed in submission order even if the dispatcher runs tasks
 * concurrently or out of order, or drops or rejects a task.
 * 
 * @since 25.1
 */
public class LocalAsynchronousSignalTree extends AsynchronousSignalTree {

    /*
     * Commands waiting to be confirmed, in submission order, and a counter
     * numbering the submitted batches. Guarded by the tree lock. Each
     * dispatched task confirms all batches up to and including its own since
     * the dispatcher is not required to preserve ordering and might also drop a
     * task.
     */
    private final ArrayDeque<List<SignalCommand>> pendingConfirmations = new ArrayDeque<>();

    private long submittedCount;

    @Override
    protected void submit(List<SignalCommand> commands) {
        assert hasLock();

        pendingConfirmations.add(commands);
        long sequence = ++submittedCount;

        try {
            SignalEnvironment.getDefaultEffectDispatcher()
                    .execute(wrapWithLock(() -> confirmUpTo(sequence)));
        } catch (RejectedExecutionException e) {
            LoggerFactory.getLogger(LocalAsynchronousSignalTree.class).debug(
                    "Confirmation task rejected, confirming immediately", e);
            confirmUpTo(sequence);
        }
    }

    private void confirmUpTo(long sequence) {
        /*
         * Keep going if a listener throws since a task that runs later might
         * already be done, which would leave the remaining batches unconfirmed.
         * The first failure is rethrown, preferring an Error over a
         * RuntimeException, with the others attached as suppressed.
         */
        Throwable failure = null;
        while (submittedCount - pendingConfirmations.size() < sequence) {
            try {
                confirm(pendingConfirmations.remove());
            } catch (RuntimeException | Error e) { // NOSONAR rethrown below
                if (failure == null) {
                    failure = e;
                } else if (failure != e) {
                    if (e instanceof Error && !(failure instanceof Error)) {
                        e.addSuppressed(failure);
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        }
        if (failure instanceof Error error) {
            throw error;
        } else if (failure != null) {
            throw (RuntimeException) failure;
        }
    }
}
