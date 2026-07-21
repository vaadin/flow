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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link UploadContent} backed by an on-disk {@link File}, used by
 * {@link AbstractFileUploadHandler} for the complete-validation phase.
 * <p>
 * Tracks every stream it hands out and is {@link AutoCloseable} so the handler
 * can close them (via try-with-resources) before deleting the file: a stream
 * left open by a validator would otherwise leak a file descriptor and, on
 * Windows, prevent deletion of a rejected upload.
 */
class FileUploadContent implements UploadContent, AutoCloseable {

    private final File file;
    private final long size;
    private final transient List<InputStream> openedStreams = new ArrayList<>(
            1);

    FileUploadContent(File file) {
        this.file = file;
        this.size = file.length();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        FileInputStream stream = new FileInputStream(file);
        openedStreams.add(stream);
        return stream;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Optional<Path> asPath() {
        return Optional.of(file.toPath());
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (InputStream stream : openedStreams) {
            try {
                stream.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        openedStreams.clear();
        if (failure != null) {
            throw failure;
        }
    }
}
