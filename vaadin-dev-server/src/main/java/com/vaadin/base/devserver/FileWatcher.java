/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * Watches for the file or sub-directory changes in the given directory.
 */
public class FileWatcher implements Serializable {
    private static final long DEFAULT_TIMEOUT = 1000;
    private final FileAlterationMonitor monitor;
    private long timeout = DEFAULT_TIMEOUT;

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
     *            the directory to watch for the changes, cannot be empty
     */
    public FileWatcher(SerializableConsumer<File> onChangeConsumer,
            File... watchDirectory) {
        this(onChangeConsumer, file -> true, watchDirectory);
    }

    /**
     * Creates an instance of the file watcher for the given directory taking
     * into account the given file filter.
     * <p>
     * Reports the changed file or directory as a {@link File} instance to the
     * provided consumer.
     * <p>
     * Watches the files create/delete and directory create/delete events.
     *
     * @param fileFilter
     *            defined if the given file or directory should be watched
     * @param onChangeConsumer
     *            to be called when any change detected
     * @param watchDirectory
     *            the directory to watch for the changes, cannot be empty
     */
    public FileWatcher(SerializableConsumer<File> onChangeConsumer,
            SerializablePredicate<File> fileFilter, File... watchDirectory) {
        this(new DefaultFileListener(onChangeConsumer), fileFilter,
                watchDirectory);
    }

    /**
     * Creates an instance of the file watcher for the given directory taking
     * into account the given file filter.
     * <p>
     * Reports the changed file or directory as a {@link File} instance to the
     * provided consumer.
     * <p>
     * Reports file and directory changes to the given listener.
     *
     * @param fileFilter
     *            defined if the given file or directory should be watched
     * @param listener
     *            to be invoked once any changes detected
     * @param watchDirectory
     *            the directory to watch for the changes, cannot be empty
     */
    public FileWatcher(FileAlterationListener listener,
            SerializablePredicate<File> fileFilter, File... watchDirectory) {
        Objects.requireNonNull(watchDirectory,
                "Watch directory cannot be empty");
        if (watchDirectory.length < 1) {
            throw new IllegalArgumentException(
                    "Watch directory cannot be empty");
        }
        Objects.requireNonNull(fileFilter, "File filter cannot be empty");
        Objects.requireNonNull(listener,
                "File alteration listener cannot be empty");
        monitor = new FileAlterationMonitor(timeout);
        Arrays.stream(watchDirectory).forEach(dir -> {
            FileAlterationObserver observer = new FileAlterationObserver(dir,
                    fileFilter::test);
            observer.addListener(listener);
            monitor.addObserver(observer);
        });
    }

    /**
     * Starts the file watching.
     *
     * @throws Exception
     *             if an error occurs during startup
     */
    public void start() throws Exception {
        monitor.start();
    }

    /**
     * Stops the file watching.
     *
     * @throws Exception
     *             if an error occurs during stop
     */
    public void stop() throws Exception {
        monitor.stop();
    }

    /**
     * Stops the file watching and waits for a given stop interval for watching
     * thread to finish.
     *
     * @param stopInterval
     *            time interval to wait for the watching thread
     * @throws Exception
     *             if an error occurs during stop
     */
    public void stop(long stopInterval) throws Exception {
        monitor.stop(stopInterval);
    }

    /**
     * Sets the time interval between file/directory checks.
     *
     * @param timeout
     *            time interval between file/directory checks
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Default file change listener which triggers the callback only when file
     * or directory is changed/deleted.
     */
    private static final class DefaultFileListener
            extends FileAlterationListenerAdaptor implements Serializable {

        private final SerializableConsumer<File> onChangeConsumer;

        public DefaultFileListener(
                SerializableConsumer<File> onChangeConsumer) {
            this.onChangeConsumer = onChangeConsumer;
        }

        @Override
        public void onDirectoryChange(File directory) {
            onChangeConsumer.accept(directory);
        }

        @Override
        public void onDirectoryDelete(File directory) {
            onChangeConsumer.accept(directory);
        }

        @Override
        public void onFileChange(File file) {
            onChangeConsumer.accept(file);
        }

        @Override
        public void onFileDelete(File file) {
            onChangeConsumer.accept(file);
        }
    }
}
