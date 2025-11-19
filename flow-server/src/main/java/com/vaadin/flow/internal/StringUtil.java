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

import com.nimbusds.jose.util.StandardCharset;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;

/**
 * Utility class for special string handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.1.4
 */
public final class StringUtil {

    public static String toUtf8Str(byte[] bytes) {
        return new String(bytes, StandardCharset.UTF_8);
    }

    public static String toUtf8Str(InputStream input) throws IOException {
        return StringUtil.toUtf8Str(input.readAllBytes());
    }

    /**
     * Comment parser state enumeration.
     */
    private enum State {
        NORMAL,
        IN_LINE_COMMENT,
        IN_BLOCK_COMMENT,
        IN_STRING,
        IN_STRING_APOSTROPHE
    }

    /**
     * Removes comments (block comments and line comments) from the JS code.
     *
     * @param code
     *            code to clean comments from
     * @return the code with removed comments
     */
    public static String removeComments(String code) {
        return removeComments(code, false);
    }

    /**
     * Removes comments (block comments and line comments) from the JS code.
     *
     * @param code
     *            code to clean comments from
     * @param useStringApostrophe
     *            if {@code true} then ' is also considered a string and
     *            comments will not be considered inside it
     * @return the code with removed comments
     */
    public static String removeComments(String code,
            boolean useStringApostrophe) {
        State state = State.NORMAL;
        StringBuilder result = new StringBuilder();
        Map<String, Character> replacements = new HashMap<>();
        Scanner scanner = new Scanner(normalize(code, replacements));
        scanner.useDelimiter("");
        while (scanner.hasNext()) {
            String character = scanner.next();
            switch (state) {
            case NORMAL:
                if (character.equals("/") && scanner.hasNext()) {
                    String nextCharacter = scanner.next();
                    if (nextCharacter.equals("/")) {
                        state = State.IN_LINE_COMMENT;
                    } else if (nextCharacter.equals("*")) {
                        state = State.IN_BLOCK_COMMENT;
                    } else {
                        result.append(character).append(nextCharacter);
                    }
                } else {
                    result.append(character);
                    if (character.equals("\"")) {
                        state = State.IN_STRING;
                    } else if (useStringApostrophe && character.equals("'")) {
                        state = State.IN_STRING_APOSTROPHE;
                    }
                }
                break;
            case IN_STRING:
                result.append(character);
                if (character.equals("\"")) {
                    state = State.NORMAL;
                } else if (character.equals("\\") && scanner.hasNext()) {
                    result.append(scanner.next());
                }
                break;
            case IN_STRING_APOSTROPHE:
                result.append(character);
                if (character.equals("'")) {
                    state = State.NORMAL;
                } else if (character.equals("\\") && scanner.hasNext()) {
                    result.append(scanner.next());
                }
                break;
            case IN_LINE_COMMENT:
                if (character.equals("\n")) {
                    result.append(character);
                    state = State.NORMAL;
                }
                break;
            case IN_BLOCK_COMMENT:
                if (character.equals("*") && scanner.hasNext("/")) {
                    scanner.next();
                    state = State.NORMAL;
                    break;
                }
            }
        }
        scanner.close();
        String handled = result.toString();
        for (Entry<String, Character> entry : replacements.entrySet()) {
            handled = handled.replace(entry.getKey(),
                    String.valueOf(entry.getValue()));
        }
        return handled;
    }

    private static String normalize(String str,
            Map<String, Character> replacements) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!Character.isHighSurrogate(ch)
                    && !Character.isLowSurrogate(ch)) {
                builder.append(str.charAt(i));
            } else {
                String replacement = generateReplacement(str);
                replacements.put(replacement, ch);
                builder.append(replacement);
            }
        }
        return builder.toString();
    }

    private static String generateReplacement(String str) {
        String replacement = UUID.randomUUID().toString();
        if (str.contains(replacement)) {
            return generateReplacement(str);
        }
        return replacement;
    }

    /**
     * Strips the given suffix from the given string, if the strings end with
     * the suffix.
     *
     * @param string
     *            the string to scan
     * @param suffix
     *            the suffix
     * @return the string without the suffix at the end or the same string if
     *         the suffix was not found
     */
    public static String stripSuffix(String string, String suffix) {
        if (string.endsWith(suffix)) {
            return string.substring(0, string.length() - suffix.length());
        }

        return string;
    }

    /**
     * Generate a hash for given content.
     *
     * @param content
     *            content to generate hash for
     * @return hash String for given content. In case content is null or empty
     *         returns empty String.
     */
    public static String getHash(String content) {
        return getHash(content, StandardCharsets.UTF_16);
    }

    /**
     * Generate hash for content using given charset for string byte encoding.
     *
     * @param content
     *            content to hash
     * @param charset
     *            charset for encoding
     * @return hash String for given content. In case content is null or empty *
     *         returns empty String.
     */
    public static String getHash(String content, Charset charset) {
        return getHash(content, null, charset);
    }

    /**
     * Generate hash for content using a given salt bytes and a given charset
     * for string byte encoding.
     *
     * @param content
     *            content to hash
     * @param salt
     *            salt to be added into hash calculation
     * @param charset
     *            charset for encoding
     * @return hash String for given content. In case content is null or empty *
     *         returns empty String.
     */
    public static String getHash(String content, byte[] salt, Charset charset) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        return bytesToHex(MessageDigestUtil.sha256(content, salt, charset));
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder result = new StringBuilder();
        for (byte hashByte : hash) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }
    
}
