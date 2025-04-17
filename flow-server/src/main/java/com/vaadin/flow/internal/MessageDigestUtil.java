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
package com.vaadin.flow.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for common {@link MessageDigest} operations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
        return sha256(string, StandardCharsets.UTF_16);
    }

    /**
     * Calculates the SHA-256 hash of the given string representation using the
     * supplied charset.
     *
     * @param string
     *            the string to hash
     *
     * @return 32 bytes making up the hash
     */
    public static byte[] sha256(String string, Charset charset) {
        return getSha256(null).digest(string.getBytes(charset));
    }

    /**
     * Calculates the SHA-256 hash of the given string with the given salt
     * representation using the supplied charset.
     *
     * @param string
     *            the string to hash
     * @param salt
     *            salt to be added into hash calculation
     *
     * @return 32 bytes making up the hash
     */
    public static byte[] sha256(String string, byte[] salt, Charset charset) {
        return getSha256(salt).digest(string.getBytes(charset));
    }

    /**
     * Calculates the SHA-256 hash of the given byte array.
     *
     * @param content
     *            the byte array to hash
     *
     * @return sha256 hash string
     */
    public static String sha256Hex(byte[] content) {
        return sha256Hex(content, null);
    }

    /**
     * Calculates the SHA-256 hash of the given byte array with the given salt.
     *
     * @param content
     *            the byte array to hash
     * @param salt
     *            salt to be added to the calculation
     * @return sha256 hash string
     */
    public static String sha256Hex(byte[] content, byte[] salt) {
        byte[] digest = getSha256(salt).digest(content);
        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            final String hex = Integer.toHexString(0xff & digest[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static MessageDigest getSha256(byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            assert digest.getDigestLength() == 32;
            if (salt != null && salt.length > 0) {
                digest.update(salt);
            }
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
