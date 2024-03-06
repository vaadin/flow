/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.nio.charset.StandardCharsets;
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
