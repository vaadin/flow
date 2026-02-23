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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalEnvironment;

/**
 * An asynchronous signal tree for single-JVM use that dispatches
 * {@link #confirm(List)} using the default effect dispatcher from
 * {@link SignalEnvironment}. This makes the behavior consistent with clustered
 * implementations where confirmation happens asynchronously.
 */
public class LocalAsynchronousSignalTree extends AsynchronousSignalTree {

    private transient Queue<List<SignalCommand>> pendingConfirmations = new ConcurrentLinkedQueue<>();

    @Override
    protected void submit(List<SignalCommand> commands) {
        pendingConfirmations.add(commands);
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(this::processPendingConfirmations);
    }

    private void processPendingConfirmations() {
        // Drain under the tree lock to guarantee FIFO confirmation order
        // even when dispatched to concurrent virtual threads.
        getLock().lock();
        try {
            List<SignalCommand> commands;
            while ((commands = pendingConfirmations.poll()) != null) {
                confirm(commands);
            }
        } finally {
            getLock().unlock();
        }
    }

    @Serial
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        pendingConfirmations = new ConcurrentLinkedQueue<>();
    }
}
