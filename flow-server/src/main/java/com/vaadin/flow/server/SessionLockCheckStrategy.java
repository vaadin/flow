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
