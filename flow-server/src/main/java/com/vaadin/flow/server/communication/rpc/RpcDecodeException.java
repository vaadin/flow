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

/**
 * Thrown if RPC method argument cannot be decoded to the required type.
 * <p>
 * It may happen when a decoder supports (applicable) to the type of argument
 * and the required type but the argument value cannot be converted to the type
 * (e.g. "1.1" can't be converted to {@link Integer} even though the
 * {@link StringToNumberDecoder} is able to decode a {@link String} to
 * {@link Integer}).
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
     *            the exception messsage
     */
    public RpcDecodeException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the given {@code cause}.
     *
     * @param cause
     *            the cause of the failed convertion
     */
    public RpcDecodeException(Throwable cause) {
        super(cause);
    }
}
