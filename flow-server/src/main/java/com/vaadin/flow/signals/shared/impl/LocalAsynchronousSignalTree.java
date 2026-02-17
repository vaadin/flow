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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vaadin.flow.signals.SignalCommand;

/**
 * An asynchronous signal tree for single-JVM use that dispatches
 * {@link #confirm(List)} on a virtual thread. This makes the behavior
 * consistent with future clustered implementations where confirmation happens
 * asynchronously. Each tree instance uses its own single-threaded executor to
 * ensure that confirmations are processed in submission order.
 */
public class LocalAsynchronousSignalTree extends AsynchronousSignalTree {
    private transient ExecutorService confirmExecutor = createExecutor();

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(
                Thread.ofVirtual().name("signal-confirm-", 1).factory());
    }

    @Override
    protected void submit(List<SignalCommand> commands) {
        confirmExecutor.execute(() -> confirm(commands));
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        confirmExecutor = createExecutor();
    }
}
