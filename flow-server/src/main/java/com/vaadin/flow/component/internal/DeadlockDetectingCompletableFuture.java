/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.vaadin.flow.server.VaadinSession;

/**
 * A completable future that will throw from blocking operations if the current
 * thread holds the session lock.
 * <p>
 * This is used for pending JS results because a response providing the value
 * cannot be processed while the session is locked.
 * <p>
 * Throwing is unfortunately only practical for this immediate instance, but
 * there isn't any sensible way of also intercepting for instances derived using
 * e.g. <code>thenAccept</code>.
 */
public class DeadlockDetectingCompletableFuture<T>
        extends CompletableFuture<T> {
    private final VaadinSession session;

    /**
     * Creates a new deadlock detecting completable future tied to the given
     * session.
     *
     * @param session
     *            the session to use, or <code>null</code> to not do any
     *            deadlock checking
     */
    public DeadlockDetectingCompletableFuture(VaadinSession session) {
        this.session = session;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throwIfDeadlock();
        return super.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        throwIfDeadlock();
        return super.get(timeout, unit);
    }

    @Override
    public T join() {
        throwIfDeadlock();
        return super.join();
    }

    private void throwIfDeadlock() {
        if (isDone()) {
            // Won't block if we're done
            return;
        }
        if (session != null && session.hasLock()) {
            /*
             * Disallow blocking if the current thread holds the lock for the
             * session that would need to be locked by a request thread to
             * complete the result
             */
            throw new IllegalStateException(
                    "Cannot do a blocking operation from the thread that has locked  the session is locked since the result cannot be made available while the session is locked.");
        }
    }
}