package com.vaadin.server;

/**
 * A Vaadin internal runtime exception thrown when the writing of the bootstrap
 * page in {@link BootstrapHandler} fails for some reason.
 *
 * @since
 */
public class BootstrapException extends RuntimeException {

    /**
     * Constructs a new bootstrap exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public BootstrapException(String message) {
        super(message);
    }

    /**
     * Constructs a new bootstrap exception with the specified detail message
     * and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i>
     * automatically incorporated in this runtime exception's detail message.
     *
     * @param message
     *            the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method).
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }

}
