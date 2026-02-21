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
package com.vaadin.flow.component.page;

import java.io.Serializable;

/**
 * Represents a file received from a clipboard paste event.
 * <p>
 * Contains the file name, MIME type, size, and raw byte data of the pasted
 * file.
 */
public class ClipboardFile implements Serializable {

    private final String fileName;
    private final String contentType;
    private final long size;
    private final byte[] data;

    /**
     * Creates a new clipboard file.
     *
     * @param fileName
     *            the name of the file
     * @param contentType
     *            the MIME type of the file
     * @param size
     *            the size of the file in bytes
     * @param data
     *            the raw byte data of the file
     */
    ClipboardFile(String fileName, String contentType, long size, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.data = data;
    }

    /**
     * Gets the name of the file.
     *
     * @return the file name
     */
    public String getName() {
        return fileName;
    }

    /**
     * Gets the MIME type of the file.
     *
     * @return the MIME type, e.g. "image/png"
     */
    public String getMimeType() {
        return contentType;
    }

    /**
     * Gets the size of the file in bytes.
     *
     * @return the file size
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets the raw byte data of the file.
     *
     * @return the file data
     */
    public byte[] getData() {
        return data;
    }
}
