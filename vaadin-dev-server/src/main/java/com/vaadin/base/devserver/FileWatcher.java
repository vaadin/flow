/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

import com.vaadin.flow.function.SerializableConsumer;

/**
 * Watches for the file or sub-directory changes in the given directory.
 */
public class FileWatcher implements Serializable {

    private Thread watchThread;

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
     */
    public FileWatcher(SerializableConsumer<File> onChangeConsumer,
            File watchDirectory) throws IOException {
        Objects.requireNonNull(watchDirectory,
                "Watch directory cannot be null");
        Objects.requireNonNull(onChangeConsumer,
                "Change listener cannot be null");
        FileSystem fileSystem = FileSystems.getDefault();
        WatchService watchService = fileSystem.newWatchService();
        watchDirectory.toPath().register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        watchThread = new Thread(() -> {
            WatchKey key;
            try {
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path path = (Path) event.context();
                        onChangeConsumer.accept(path.toFile());
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {

            }
        });
    }

    /**
     * Starts the file watching.
     *
     * @throws Exception
     *             if an error occurs during startup
     */
    public void start() throws Exception {
        watchThread.start();
    }

    /**
     * Stops the file watching.
     *
     * @throws Exception
     *             if an error occurs during stop
     */
    public void stop() throws Exception {
        watchThread.interrupt();
    }

}
