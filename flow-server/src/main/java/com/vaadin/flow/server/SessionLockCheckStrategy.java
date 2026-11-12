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
package com.vaadin.flow.server;

import java.util.function.BooleanSupplier;

/**
 * Available strategies for session lock checking.
 * 
 * @since 24.4
 */
public enum SessionLockCheckStrategy {
    /**
     * The default strategy, runs Java `assert` statement. Does nothing when
     * assertions are disabled (the default for JVM).
     */
    ASSERT {
        @Override
        public void checkHasLock(BooleanSupplier hasLock, VaadinSession session,
                String message) {
            assert hasLock.getAsBoolean() : message;
        }
    },
    /**
     * If the session doesn't have a lock, a warning message is logged to the
     * log but the code execution continues normally.
     */
    LOG {
        @Override
        public void checkHasLock(BooleanSupplier hasLock, VaadinSession session,
                String message) {
            if (!hasLock.getAsBoolean()) {
                session.getLogger().warn(message,
                        new IllegalStateException(message));
            }
        }
    },
    /**
     * If the session doesn't have a lock, an {@link IllegalStateException} is
     * thrown.
     */
    THROW {
        @Override
        public void checkHasLock(BooleanSupplier hasLock, VaadinSession session,
                String message) {
            if (!hasLock.getAsBoolean()) {
                throw new IllegalStateException(message);
            }
        }
    };

    /**
     * Potentially checks whether this session is currently locked by the
     * current thread. With {@link VaadinSession#isUILockingEnabled() UI
     * locking}, holding the lock of one of the session's UIs is also accepted,
     * see {@link VaadinSession#hasAccess()}.
     *
     * @param session
     *            the session to check the lock for, not null.
     * @param message
     *            the error message to include when failing if the check is done
     *            and the session is not locked
     */
    public void checkHasLock(VaadinSession session, String message) {
        checkHasLock(session::hasAccess, session, message);
    }

    /**
     * Potentially checks whether the current thread holds a lock, and fails
     * according to this strategy if not.
     *
     * @param hasLock
     *            evaluates whether the required lock is held, only invoked if
     *            the check is done, not null
     * @param session
     *            the session the lock belongs to, not null
     * @param message
     *            the error message to include when failing if the check is done
     *            and the lock is not held
     */
    public abstract void checkHasLock(BooleanSupplier hasLock,
            VaadinSession session, String message);
}
