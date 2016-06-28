/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A pool of immutable JSON values known by the client.
 *
 * @author Vaadin Ltd
 */
public class ConstantPool implements Serializable {

    private Set<String> knownValues = new HashSet<>();

    private Map<String, JsonValue> newValues = new HashMap<>();

    /**
     * Gets the id of a given constant, registering the constant with this
     * constant pool if it hasn't already been encountered.
     *
     * @see #dumpConstants()
     *
     * @param constant
     *            the constant reference to get an id for, not <code>null</code>
     * @return the constant id of the given constant, not <code>null</code>
     */
    public String getConstantId(ConstantPoolReference<?> constant) {
        assert constant != null;

        JsonValue fullJson = constant.getJson();

        String key = calculateKey(fullJson);

        if (knownValues.add(key)) {
            JsonValue oldValue = newValues.put(key, fullJson);

            assert oldValue == null
                    || oldValue.toJson().equals(fullJson.toJson());
        }

        return key;
    }

    /**
     * Checks if any new constants have been added to this constant pool since
     * the last time {@link #dumpConstants()} was called.
     *
     * @return <code>true</code> if there are new constants, <code>false</code>
     *         otherwise
     */
    public boolean hasNewConstants() {
        return !newValues.isEmpty();
    }

    /**
     * Encodes all new constants to a JSON object and marks those constants as
     * non-new.
     *
     * @return a JSON object describing all new constants
     */
    public JsonObject dumpConstants() {
        JsonObject json = Json.createObject();

        newValues.forEach(json::put);
        newValues.clear();

        return json;
    }

    /**
     * Calculates the key of a JSON value by Base 64 encoding the first 128 bits
     * of the SHA-256 digest of the JSON's string representation.
     *
     * @param json
     *            the JSON to get a hash of, not <code>null</code>
     * @return the key uniquely identifying the given JSON value
     */
    private static String calculateKey(JsonValue json) {
        byte[] jsonBytes = json.toJson().getBytes(StandardCharsets.UTF_16);
        byte[] digest = getMessageDigest().digest(jsonBytes);

        /*
         * Only use first 64 bits to keep id string short (1 in 100 000 000
         * collision risk with 500 000 items). 64 bits base64 -> 11 chars
         */
        ByteBuffer truncatedDigest = ByteBuffer.wrap(digest, 0, 8);

        ByteBuffer base64Bytes = Base64.getEncoder().encode(truncatedDigest);

        return StandardCharsets.US_ASCII.decode(base64Bytes).toString();
    }

    private static MessageDigest getMessageDigest() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            assert digest.getDigestLength() == 32;
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(
                    "Every implementation of the Java platform is required to support SHA-256",
                    e);
        }
    }

}
