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

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A value that has two parts: a shared part, and a part that is specific to
 * this instance.
 * <p>
 * The shared part is sent through the constant pool, so the client gets it only
 * once. The instance specific part is sent as it is. Use this for values that
 * are almost the same for all instances, with only a small part that changes.
 * If the changing part were in the constant pool, every instance would add a
 * new constant, and nothing would be shared.
 * <p>
 * The value is encoded as an array with two items:
 * <ol>
 * <li>the constant pool id of the shared part, and</li>
 * <li>the instance specific part.</li>
 * </ol>
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class ParameterizedConstantPoolKey implements Serializable {

    private final ConstantPoolKey sharedValue;
    private final ObjectNode parameters;

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
            ObjectNode parameters) {
        this.sharedValue = Objects.requireNonNull(sharedValue);
        this.parameters = Objects.requireNonNull(parameters);
    }

    /**
     * Encodes this value for the client. Adds the shared value to the given
     * constant pool.
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
