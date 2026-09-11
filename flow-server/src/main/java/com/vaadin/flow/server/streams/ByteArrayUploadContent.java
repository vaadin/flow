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
package com.vaadin.flow.server.streams;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * {@link UploadContent} backed by an in-memory {@code byte[]}, used by
 * {@link InMemoryUploadHandler} for the complete-validation phase. Reuses the
 * existing array (no copy) and holds no OS resource.
 */
class ByteArrayUploadContent implements UploadContent {

    private final byte[] data;

    // Intentionally references the given array without copying: this
    // package-private type is constructed only by InMemoryUploadHandler with a
    // freshly created array it owns, and never exposes it (getInputStream wraps
    // it read-only). Copying would double the memory of the whole upload.
    @SuppressWarnings("java:S2384")
    ByteArrayUploadContent(byte[] data) {
        this.data = data;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public long size() {
        return data.length;
    }

    @Override
    public Optional<Path> asPath() {
        return Optional.empty();
    }
}
