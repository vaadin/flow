/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import java.io.Serializable;

import elemental.json.JsonValue;

/**
 * Decoder of RPC method arguments (server-side methods invoked from the
 * client-side).
 * <p>
 * The client-side argument type and the server-side argument type doesn't have
 * to match. The decoders are applied to be able to handle arguments whose types
 * on the server side and on the client side are different.
 * <p>
 * Each decoder is checked whether it's may be used to handle the argument value
 * with the required server-side parameter type via the
 * {@link #isApplicable(JsonValue, Class)} method. Decoder is applied to the
 * received value and required type if it's applicable.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface RpcDecoder extends Serializable {

    /**
     * Returns {@code true} if the decoder is applicable for the given
     * {@code value} and the required {@code type}.
     *
     * @param value
     *            the value which needs to be decoded
     * @param type
     *            the required type to decode
     * @return {@code true} if this decoder is able to decode the {@code value}
     *         to the {@code type}, {@code false} otherwise
     */
    boolean isApplicable(JsonValue value, Class<?> type);

    /**
     * Decode the given {@code value} to the required {@code type}.
     * <p>
     * {@link RpcDecodeException} is thrown if the {@code value} cannot be
     * converted to the {@code type} (even though the decoder is applicable for
     * the {@code value} and the {@code type}).
     *
     * @param value
     *            the value which needs to be decoded
     * @param type
     *            the required type to decode
     * @param <T>
     *            the decoded value type
     * @return the decoded value
     * @throws RpcDecodeException
     *             if the {@code value} cannot be converted to the {@code type}
     */
    <T> T decode(JsonValue value, Class<T> type) throws RpcDecodeException;
}
