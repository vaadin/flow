/*
 * Copyright 2000-2018 Vaadin Ltd.
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
