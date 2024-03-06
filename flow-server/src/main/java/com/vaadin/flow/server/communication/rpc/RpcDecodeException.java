/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

/**
 * Thrown if RPC method argument cannot be decoded to the required type.
 * <p>
 * It may happen when a decoder supports (applicable) to the type of argument
 * and the required type but the argument value cannot be converted to the type
 * (e.g. "1.1" can't be converted to {@link Integer} even though the
 * {@link StringToNumberDecoder} is able to decode a {@link String} to
 * {@link Integer}).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class RpcDecodeException extends Exception {

    /**
     * Creates a new instance with the given {@code message}.
     *
     * @param message
     *            the exception message
     */
    public RpcDecodeException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the given {@code cause}.
     *
     * @param cause
     *            the cause of the failed conversion
     */
    public RpcDecodeException(Throwable cause) {
        super(cause);
    }
}
