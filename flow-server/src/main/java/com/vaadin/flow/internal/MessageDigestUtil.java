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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for common {@link MessageDigest} operations.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MessageDigestUtil {

    private MessageDigestUtil() {
        // Static helpers only
    }

    /**
     * Calculates the SHA-256 hash of the UTF-16 representation of the given
     * string.
     *
     * @param string
     *            the string to hash
     *
     * @return 32 bytes making up the hash
     */
    public static byte[] sha256(String string) {
        return getSha256().digest(string.getBytes(StandardCharsets.UTF_16));
    }

    private static MessageDigest getSha256() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            assert digest.getDigestLength() == 32;
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(
                    "Your Java implementation does not support SHA-256, "
                            + "even though it is required by the Java specification. "
                            + "Change to an implementation which follows the specification.",
                    e);
        }
    }

}
