/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

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

    /**
     * Comment parser state enumeration.
     */
    private enum State {
        NORMAL, IN_LINE_COMMENT, IN_BLOCK_COMMENT, IN_STRING, IN_STRING_APOSTROPHE
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
     * Generate a hash for given content.
     *
     * @param content
     *            content to generate hash for
     * @return hash String for given content. In case content is null or empty
     *         returns empty String.
     */
    public static String getHash(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        return bytesToHex(MessageDigestUtil.sha256(content));
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
