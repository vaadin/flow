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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for parsing X-Vaadin-Upload-Metadata HTTP headers.
 * <p>
 * The X-Vaadin-Upload-Metadata header uses URL query string format to encode
 * file upload metadata (e.g., {@code name=file.txt&size=1024}). All values are
 * percent-encoded for proper handling of special characters.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public final class UploadMetadataParser implements Serializable {

    private UploadMetadataParser() {
        // Utility class, prevent instantiation
    }

    /**
     * Parses the X-Vaadin-Upload-Metadata header value into a map of key-value
     * pairs.
     * <p>
     * The header format is URL query string format: {@code key1=value1&key2=value2}.
     * All values are automatically percent-decoded.
     *
     * @param metadataHeader
     *            the X-Vaadin-Upload-Metadata header value
     * @return a map of decoded metadata key-value pairs, or an empty map if
     *         the header is null or empty
     */
    public static Map<String, String> parse(String metadataHeader) {
        Map<String, String> metadata = new HashMap<>();

        if (metadataHeader == null || metadataHeader.isEmpty()) {
            return metadata;
        }

        String[] pairs = metadataHeader.split("&");
        for (String pair : pairs) {
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex == -1) {
                // No '=' found, skip this pair
                continue;
            }

            String key = pair.substring(0, equalsIndex);
            String value = pair.substring(equalsIndex + 1);

            // Decode percent-encoded values
            try {
                key = URLDecoder.decode(key, StandardCharsets.UTF_8.name());
                value = URLDecoder.decode(value,
                        StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // UTF-8 is always supported, but URLDecoder.decode declares
                // the exception
            }

            metadata.put(key, value);
        }

        return metadata;
    }

    /**
     * Extracts the filename from the X-Vaadin-Upload-Metadata header.
     * <p>
     * The filename is stored under the {@code name} key in the metadata.
     *
     * @param metadataHeader
     *            the X-Vaadin-Upload-Metadata header value
     * @return the decoded filename, or {@code null} if not found
     */
    public static String extractFilename(String metadataHeader) {
        Map<String, String> metadata = parse(metadataHeader);
        return metadata.get("name");
    }
}
