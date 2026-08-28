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
package com.vaadin.flow.devloop.daemon;

import java.util.Optional;

/**
 * Just enough JSON to emit the daemon's own output, and to read one string out
 * of somebody else's, without a dependency.
 */
final class Json {

    private Json() {
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '"' -> sb.append("\\\"");
            case '\\' -> sb.append("\\\\");
            case '\n' -> sb.append("\\n");
            case '\r' -> sb.append("\\r");
            case '\t' -> sb.append("\\t");
            default -> {
                if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
            }
        }
        return sb.toString();
    }

    static String array(java.util.List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    static String strings(java.util.List<String> items) {
        return array(items.stream().map(s -> "\"" + escape(s) + "\"").toList());
    }

    /**
     * The string value of a key in a flat JSON object.
     * <p>
     * A reader rather than a parser, because the one thing the daemon has to
     * read from someone else's JSON is a path out of Flow's
     * {@code flow-build-info.json}, and a parser would be several hundred lines
     * to answer that. It is deliberately conservative: anything it does not
     * recognise - a nested object, a number, a truncated file - is reported as
     * absent rather than guessed at, and the caller falls back.
     * <p>
     * The opening quote is part of the search, so {@code "frontend"} cannot
     * match {@code "frontendFolder"}. Unescaping is not optional either: the
     * value is a Windows path, and {@code C:\\Projects} read literally names
     * nothing.
     *
     * @param json
     *            the JSON text, may be null
     * @param key
     *            the key to look for
     * @return the unescaped value, or empty if it is absent or not a string
     */
    static Optional<String> stringField(String json, String key) {
        if (json == null || key == null) {
            return Optional.empty();
        }
        int at = json.indexOf("\"" + key + "\"");
        if (at < 0) {
            return Optional.empty();
        }
        int i = skipWhitespace(json, at + key.length() + 2);
        if (i >= json.length() || json.charAt(i) != ':') {
            return Optional.empty();
        }
        i = skipWhitespace(json, i + 1);
        if (i >= json.length() || json.charAt(i) != '"') {
            // A number, an object, null - all "not a string", all the caller's
            // signal to fall back rather than to fail.
            return Optional.empty();
        }
        return unescape(json, i + 1);
    }

    private static int skipWhitespace(String json, int from) {
        int i = from;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Reads a JSON string body, starting after its opening quote. */
    private static Optional<String> unescape(String json, int from) {
        StringBuilder value = new StringBuilder();
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                return Optional.of(value.toString());
            }
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (++i >= json.length()) {
                break;
            }
            switch (json.charAt(i)) {
            case '"' -> value.append('"');
            case '\\' -> value.append('\\');
            case '/' -> value.append('/');
            case 'b' -> value.append('\b');
            case 'f' -> value.append('\f');
            case 'n' -> value.append('\n');
            case 'r' -> value.append('\r');
            case 't' -> value.append('\t');
            case 'u' -> {
                if (i + 4 >= json.length()) {
                    return Optional.empty();
                }
                try {
                    value.append((char) Integer
                            .parseInt(json.substring(i + 1, i + 5), 16));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
                i += 4;
            }
            default -> {
                return Optional.empty();
            }
            }
        }
        // Unterminated: the file was truncated mid-write, which a concurrent
        // Maven run can produce. Absent, not half a path.
        return Optional.empty();
    }
}
