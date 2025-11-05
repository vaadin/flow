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
package com.vaadin.flow.server.streams;

/**
 * Utility class for parsing Content-Disposition HTTP headers.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public final class ContentDispositionParser {

    private ContentDispositionParser() {
        // Utility class, prevent instantiation
    }

    /**
     * Extracts the filename from a Content-Disposition header value.
     * <p>
     * Supports both standard filename parameter (e.g.,
     * {@code filename="file.txt"}) and RFC 5987 extended filename parameter
     * (e.g., {@code filename*=UTF-8''file.txt}).
     *
     * @param contentDisposition
     *            the Content-Disposition header value
     * @return the extracted filename, or {@code null} if not found
     */
    public static String extractFilename(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }

        // Try to extract filename* first (RFC 5987 extended format)
        String filename = extractParameter(contentDisposition, "filename*");
        if (filename != null) {
            // filename* format: charset'lang'value or charset''value
            int firstQuote = filename.indexOf('\'');
            if (firstQuote != -1) {
                int secondQuote = filename.indexOf('\'', firstQuote + 1);
                if (secondQuote != -1 && secondQuote < filename.length() - 1) {
                    return filename.substring(secondQuote + 1);
                }
            }
        }

        // Fall back to standard filename parameter
        filename = extractParameter(contentDisposition, "filename");
        return filename;
    }

    /**
     * Extracts a parameter value from a Content-Disposition header.
     *
     * @param headerValue
     *            the header value to parse
     * @param paramName
     *            the parameter name to extract
     * @return the parameter value (unquoted), or {@code null} if not found
     */
    public static String extractParameter(String headerValue,
            String paramName) {
        if (headerValue == null || paramName == null) {
            return null;
        }

        String searchFor = paramName + "=";
        int paramIndex = headerValue.indexOf(searchFor);
        if (paramIndex == -1) {
            return null;
        }

        int valueStart = paramIndex + searchFor.length();
        if (valueStart >= headerValue.length()) {
            return "";
        }

        String value;
        char firstChar = headerValue.charAt(valueStart);
        if (firstChar == '"') {
            // Quoted value
            int endQuote = headerValue.indexOf('"', valueStart + 1);
            if (endQuote == -1) {
                // No closing quote, take rest of string
                value = headerValue.substring(valueStart + 1);
            } else {
                value = headerValue.substring(valueStart + 1, endQuote);
            }
        } else {
            // Unquoted value, find next semicolon or end of string
            int semicolon = headerValue.indexOf(';', valueStart);
            if (semicolon == -1) {
                value = headerValue.substring(valueStart);
            } else {
                value = headerValue.substring(valueStart, semicolon);
            }
        }

        return value.trim();
    }
}
