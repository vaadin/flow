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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Wraps a JSON value that should be stored in the {@link ConstantPool} shared
 * between the client and the server. A constant pool key stored as a value in a
 * state node will be encoded as a string containing the id that the client can
 * use for fetching the actual JSON value from the client-side constant pool.
 * This is a way of deduplicating JSON values that are expected to be sent to
 * the same client multiple times, since all references to the same JSON
 * structure will be encoded as the same id.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ConstantPoolKey implements Serializable {
    private final JsonValue json;
    private final String id;

    /**
     * Creates a new constant pool key for the given JSON value. The value
     * should not be modified after this instance has been created since it
     * might cause the constant pool value to be inconsistent.
     *
     * @param json
     *            the JSON constant, not <code>null</code>
     */
    public ConstantPoolKey(JsonValue json) {
        assert json != null;
        this.json = json;

        id = calculateHash(json);
    }

    /**
     * Gets the id used to identify the referenced JSON constant.
     *
     * @return the id used to identify this value
     */
    public String getId() {
        return id;
    }

    /**
     * Exports the this key into a JSON object to send to the client. This
     * method should be called only by the {@link ConstantPool} instance that
     * manages this value. It may be called multiple times.
     *
     * @param clientConstantPoolUpdate
     *            the constant pool update that is to be sent to the client, not
     *            <code>null</code>
     */
    public void export(JsonObject clientConstantPoolUpdate) {
        assert id.equals(calculateHash(json)) : "Json value has been changed";

        clientConstantPoolUpdate.put(id, json);
    }

    /**
     * Calculates the key of a JSON value by Base 64 encoding the first 64 bits
     * of the SHA-256 digest of the JSON's string representation.
     *
     * @param json
     *            the JSON to get a hash of, not <code>null</code>
     * @return the key uniquely identifying the given JSON value
     */
    private static String calculateHash(JsonValue json) {
        byte[] digest = MessageDigestUtil.sha256(json.toJson());

        /*
         * Only use first 64 bits to keep id string short (1 in 100 000 000
         * collision risk with 500 000 items). 64 bits base64 -> 11 ASCII chars
         */
        ByteBuffer truncatedDigest = ByteBuffer.wrap(digest, 0, 8);

        ByteBuffer base64Bytes = Base64.getEncoder().encode(truncatedDigest);

        return StandardCharsets.US_ASCII.decode(base64Bytes).toString();
    }

}
