/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.internal.CurrentInstance;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockVaadinSession extends VaadinSession {
    /*
     * Used to make sure there's at least one reference to the mock session
     * while it's locked. This is used to prevent the session from being eaten
     * by GC in tests where @Before creates a session and sets it as the current
     * instance without keeping any direct reference to it. This pattern has a
     * chance of leaking memory if the session is not unlocked in the right way,
     * but it should be acceptable for testing use.
     */
    private static final ThreadLocal<MockVaadinSession> referenceKeeper = new ThreadLocal<>();

    public MockVaadinSession(VaadinService service) {
        super(service);
    }

    public MockVaadinSession() {
        this(new MockVaadinServletService());
    }

    @Override
    public void close() {
        super.close();
        closeCount++;
    }

    public int getCloseCount() {
        return closeCount;
    }

    @Override
    public Lock getLockInstance() {
        return lock;
    }

    @Override
    public void lock() {
        super.lock();
        referenceKeeper.set(this);
    }

    @Override
    public void unlock() {
        super.unlock();
        referenceKeeper.remove();
    }

    private int closeCount;

    private ReentrantLock lock = new ReentrantLock();

    public <T> T runWithLock(Callable<T> action) throws Exception {
        Map<Class<?>, CurrentInstance> previous = CurrentInstance
                .setCurrent(this);
        lock();
        try {
            return action.call();
        } finally {
            unlock();
            CurrentInstance.restoreInstances(previous);
        }
    }
}
