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
 * Metadata for successful upload.
 * <p>
 * The fileName and contentType will only be available for multipart uploads.
 *
 * @param fileName
 *            the name of the file, may be <code>null</code>
 * @param contentType
 *            the content type, may be <code>null</code>
 * @param contentLength
 *            the content length in bytes
 */
public record UploadMetadata(String fileName, String contentType,
        long contentLength) {
}
