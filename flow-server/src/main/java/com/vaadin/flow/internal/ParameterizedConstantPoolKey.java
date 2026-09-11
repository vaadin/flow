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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.Objects;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/**
 * A value that is sent to the client as a reference to a shared constant
 * together with data that is specific to this instance.
 * <p>
 * The shared part is deduplicated through the constant pool in the usual way,
 * while the instance specific part is sent as-is. This is used for values that
 * are mostly identical between instances but where a small part varies, since
 * putting the varying part in the constant pool would defeat its purpose of
 * sending shared data only once.
 * <p>
 * The value is encoded as a two element array where the first item is the
 * constant pool id of the shared value and the second item is the instance
 * specific data.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 25.3
 */
public class ParameterizedConstantPoolKey implements Serializable {

    private final ConstantPoolKey sharedValue;
    private final JsonNode parameters;

    /**
     * Creates a new parameterized constant pool key.
     *
     * @param sharedValue
     *            the shared value to send through the constant pool, not
     *            <code>null</code>
     * @param parameters
     *            the instance specific data to send as-is, not
     *            <code>null</code>
     */
    public ParameterizedConstantPoolKey(ConstantPoolKey sharedValue,
            JsonNode parameters) {
        this.sharedValue = Objects.requireNonNull(sharedValue);
        this.parameters = Objects.requireNonNull(parameters);
    }

    /**
     * Encodes this value for the client, registering the shared value with the
     * given constant pool.
     *
     * @param constantPool
     *            the constant pool to use for the shared value, not
     *            <code>null</code>
     * @return the encoded value
     */
    public ArrayNode encode(ConstantPool constantPool) {
        ArrayNode json = JacksonUtils.createArrayNode();
        json.add(constantPool.getConstantId(sharedValue));
        json.add(parameters);
        return json;
    }
}
