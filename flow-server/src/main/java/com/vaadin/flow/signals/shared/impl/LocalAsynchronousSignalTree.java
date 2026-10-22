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

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalEnvironment;

/**
 * An asynchronous signal tree for single-JVM use that dispatches
 * {@link #confirm(List)} using the default effect dispatcher from
 * {@link SignalEnvironment}. This makes the behavior consistent with clustered
 * implementations where confirmation happens asynchronously. Submitted commands
 * are confirmed in submission order even if the dispatcher runs tasks
 * concurrently or out of order.
 * 
 * @since 25.1
 */
public class LocalAsynchronousSignalTree extends AsynchronousSignalTree {

    /*
     * Commands waiting to be confirmed, in submission order. Guarded by the
     * tree lock. Each dispatched task confirms the oldest entry rather than the
     * commands it was created for since the dispatcher is not required to
     * preserve ordering.
     */
    private final ArrayDeque<List<SignalCommand>> pendingConfirmations = new ArrayDeque<>();

    @Override
    protected void submit(List<SignalCommand> commands) {
        assert hasLock();

        pendingConfirmations.add(commands);

        try {
            SignalEnvironment.getDefaultEffectDispatcher().execute(
                    wrapWithLock(() -> confirm(pendingConfirmations.remove())));
        } catch (RuntimeException e) {
            /*
             * Drop the entry if no task was scheduled for it so that later
             * tasks don't fall one batch behind. An immediate dispatcher may
             * already have consumed it before failing.
             */
            if (pendingConfirmations.peekLast() == commands) {
                pendingConfirmations.removeLast();
            }
            throw e;
        }
    }
}
