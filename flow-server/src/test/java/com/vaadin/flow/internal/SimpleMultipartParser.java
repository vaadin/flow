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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Simple multipart response parser for testing purposes only.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
class SimpleMultipartParser {

    private final BufferedReader reader;
    private final String boundary;

    public SimpleMultipartParser(byte[] data, String boundary) {
        this.reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(data), StandardCharsets.UTF_8));
        this.boundary = "--" + boundary;
    }

    private boolean lastPartRead = false;
    private String pushedBackLine = null;

    /**
     * Reads the headers of the next part.
     *
     * @return the headers as a string, or null if no more parts
     * @throws IOException
     *             if an I/O error occurs
     */
    public String readHeaders() throws IOException {
        if (lastPartRead) {
            return null;
        }

        StringBuilder headers = new StringBuilder();
        String line;

        // Use pushed back line if available, otherwise read
        if (pushedBackLine != null) {
            line = pushedBackLine;
            pushedBackLine = null;
        } else {
            // Skip to boundary
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(boundary)) {
                    break;
                }
            }
        }

        if (line == null) {
            return null;
        }

        // Check if this is the final boundary
        if (line.equals(boundary + "--")) {
            lastPartRead = true;
            return null;
        }

        // Read headers until empty line
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            headers.append(line).append("\n");
        }

        return headers.toString();
    }

    /**
     * Reads the body data of the current part.
     *
     * @param outputStream
     *            the output stream to write the body data to
     * @throws IOException
     *             if an I/O error occurs
     */
    public void readBodyData(ByteArrayOutputStream outputStream)
            throws IOException {
        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(boundary)) {
                // Found the next boundary, push it back for next readHeaders
                // call
                pushedBackLine = line;

                // Check if this is the final boundary
                if (line.equals(boundary + "--")) {
                    lastPartRead = true;
                }
                return;
            }

            // Add CRLF before each line except the first
            if (!firstLine) {
                outputStream.write('\r');
                outputStream.write('\n');
            }
            firstLine = false;

            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        }
    }
}
