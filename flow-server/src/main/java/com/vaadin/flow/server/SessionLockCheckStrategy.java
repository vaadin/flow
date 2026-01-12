/*
 * Copyright 2000-2025 Vaadin Ltd.
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

/**
 * Available strategies for session lock checking.
 */
public enum SessionLockCheckStrategy {
    /**
     * The default strategy, runs Java `assert` statement. Does nothing when
     * assertions are disabled (the default for JVM).
     */
    ASSERT {
        @Override
        public void checkHasLock(VaadinSession session, String message) {
            assert session.hasLock() : message;
        }
    },
    /**
     * If the session doesn't have a lock, a warning message is logged to the
     * log but the code execution continues normally.
     */
    LOG {
        @Override
        public void checkHasLock(VaadinSession session, String message) {
            if (!session.hasLock()) {
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
        public void checkHasLock(VaadinSession session, String message) {
            if (!session.hasLock()) {
                throw new IllegalStateException(message);
            }
        }
    };

    /**
     * Potentially checks whether this session is currently locked by the
     * current thread
     *
     * @param session
     *            the session to check the lock for, not null.
     * @param message
     *            the error message to include when failing if the check is done
     *            and the session is not locked
     */
    public abstract void checkHasLock(VaadinSession session, String message);
}
