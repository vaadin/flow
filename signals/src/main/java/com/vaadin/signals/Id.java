/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Generated identifier for signals and other related resources.
 * <p>
 * The id is a random 64-bit number to be more compact than a full 128-bit UUID
 * or such. The ids don't need to be globally unique but only unique within a
 * smaller context so the risk of collisions is still negligible. The value is
 * JSON serialized as a base64-encoded string with a special case,
 * <code>""</code>, for the frequently used special 0 id. The ids are comparable
 * to facilitate consistent ordering to avoid deadlocks in certain situations.
 *
 * @param value
 *            the id value as a 64-bit integer
 */
public record Id(long value) implements Comparable<Id> {
    /**
     * Default or initial id in various contexts. Always used for the root node
     * in a signal hierarchy. The zero id is frequently used and has a custom
     * compact JSON representation.
     */
    public static final Id ZERO = new Id(0);

    /**
     * Id representing the edge of a list. This can be either the position
     * immediately before the first entry or the position immediately after the
     * last entry.
     */
    public static final Id EDGE = ZERO;

    /**
     * Special id value reserved for internal bookkeeping.
     */
    public static final Id MAX = new Id(Long.MAX_VALUE);

    /*
     * Padding refers to the trailing '=' characters that are only necessary
     * when base64 values are concatenated together
     */
    private static final Encoder base64Encoder = Base64.getEncoder()
            .withoutPadding();

    /**
     * Creates a random id. Randomness is only needed to reduce the risk of
     * collisions but there's no security impact from being able to guess random
     * ids.
     *
     * @return a random id, not <code>null</code>
     */
    public static Id random() {
        var random = ThreadLocalRandom.current();

        long value;
        do {
            value = random.nextLong();
        } while (value == 0 || value == Long.MAX_VALUE);

        return new Id(value);
    }

    /**
     * Parses the given base64 string as an id. As a special case, the empty
     * string is parsed as {@link #ZERO}.
     *
     * @param base64
     *            the base64 string to parse, not <code>null</code>
     * @return the parsed id.
     */
    @JsonCreator
    public static Id parse(String base64) {
        if (base64.equals("")) {
            return ZERO;
        }
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new Id(new BigInteger(bytes).longValue());
    }

    /**
     * Returns this id value as a base64 string.
     *
     * @return the base64 string representing this id
     */
    @JsonValue
    public final String asBase64() {
        if (value == 0) {
            return "";
        }
        byte[] bytes = BigInteger.valueOf(value).toByteArray();
        return base64Encoder.encodeToString(bytes);
    }

    @Override
    public int compareTo(Id other) {
        return Long.compare(value, other.value);
    }
}
