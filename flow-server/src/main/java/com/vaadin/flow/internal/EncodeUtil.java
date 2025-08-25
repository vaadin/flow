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

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utilities related to various encoding schemes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class EncodeUtil {

    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    private EncodeUtil() {
        // Static utils only
    }

    /**
     * Encodes the given string to UTF-8 <code>value-chars</code> as defined in
     * RFC5987 for use in e.g. the <code>Content-Disposition</code> HTTP header.
     *
     * @param value
     *            the string to encode, not <code>null</code>
     * @return the encoded string
     */
    public static String rfc5987Encode(String value) {
        StringBuilder builder = new StringBuilder();

        int i = 0;
        while (i < value.length()) {
            int cp = value.codePointAt(i);
            if (cp < 127 && isRFC5987AttrChar(cp)) {
                builder.append((char) cp);
            } else {
                // Create string from a single code point
                String cpAsString = new String(new int[] { cp }, 0, 1);

                appendHexBytes(builder, cpAsString.getBytes(UTF_8));
            }

            // Advance to the next code point
            i += Character.charCount(cp);
        }

        return builder.toString();
    }

    private static void appendHexBytes(StringBuilder builder, byte[] bytes) {
        for (byte byteValue : bytes) {
            builder.append('%');
            HEX_FORMAT.toHexDigits(builder, byteValue);
        }
    }

    private static boolean isRFC5987AttrChar(int codePoint) {
        return Character.isLetterOrDigit(codePoint)
                || "!#$&+-.^_`|~".indexOf(codePoint) >= 0;
    }

    /**
     * Encodes the given header field param as defined in RFC 2047 for use in
     * e.g. the <code>Content-Disposition</code> HTTP header.
     *
     * @param value
     *            the string to encode, not <code>null</code>
     * @return the encoded string
     */
    public static String rfc2047Encode(String value) {
        byte[] source = value.getBytes(UTF_8);
        StringBuilder sb = new StringBuilder(source.length << 1);
        sb.append("=?").append(UTF_8.name()).append("?Q?");
        for (byte b : source) {
            if (b == 32) {
                sb.append('_'); // Replace space with underscore
            } else if (isPrintable(b)) {
                sb.append((char) b);
            } else {
                sb.append('=');
                HEX_FORMAT.toHexDigits(sb, b);
            }
        }
        sb.append("?=");
        return sb.toString();
    }

    private static boolean isPrintable(byte c) {
        int b = c & 0xFF; // Convert to unsigned
        // RFC 2045, Section 6.7, and RFC 2047, Section 4.2
        // printable characters are 33-126, excluding "=?_
        return (b >= 33 && b <= 126) && b != 34 && b != 61 && b != 63
                && b != 95;
    }

    /**
     * Checks if the given string contains only US-ASCII characters.
     *
     * @param text
     *            the string to check, not <code>null</code>
     * @return <code>true</code> if the string contains only US-ASCII
     *         characters,
     */
    public static boolean isPureUSASCII(String text) {
        return StandardCharsets.US_ASCII.newEncoder().canEncode(text);
    }
}
