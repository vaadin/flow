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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableConsumer;

/**
 * Watches for the file or sub-directory changes in the given directory.
 */
public class FileWatcher {

    private DirectoryWatcher watcher;

    private static final ExecutorService executorService = Executors
            .newCachedThreadPool(
                    new NamedDaemonThreadFactory("vaadin-file-watcher"));

    /**
     * Creates an instance of the file watcher for the given directory.
     * <p>
     * Reports the changed file or directory as a {@link File} instance to the
     * provided consumer.
     * <p>
     * Watches the files create/delete and directory create/delete events.
     *
     * @param onChangeConsumer
     *            to be called when any change detected
     * @param watchDirectory
     *            the directory to watch for changes, cannot be empty
     * @throws IOException
     *             if an error occurs while setting up the watcher
     */
    public FileWatcher(SerializableConsumer<File> onChangeConsumer,
            File watchDirectory) throws IOException {
        Objects.requireNonNull(watchDirectory,
                "Watch directory cannot be null");
        Objects.requireNonNull(onChangeConsumer,
                "Change listener cannot be null");
        watcher = DirectoryWatcher.builder().path(watchDirectory.toPath())
                .listener(e -> {
                    onChangeConsumer.accept(e.path().toFile());
                }).build();
    }

    /**
     * Starts the file watching.
     */
    public void start() {
        watcher.watchAsync(executorService).exceptionally((e) -> {
            getLogger().error("Error starting file watcher", e);
            return null;
        });
    }

    /**
     * Stops the file watching.
     *
     * @throws IOException
     *             if an error occurs during stop
     */
    public void stop() throws IOException {
        watcher.close();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FileWatcher.class);
    }

}
