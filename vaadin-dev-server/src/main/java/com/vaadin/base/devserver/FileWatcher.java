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
import java.util.Objects;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializablePredicate;

public class FileWatcher implements Serializable {
    private static final long DEFAULT_TIMEOUT = 1000;
    private final FileAlterationMonitor monitor;
    private long timeout = DEFAULT_TIMEOUT;

    public FileWatcher(File watchDirectory,
            SerializableConsumer<File> onChangeConsumer) {
        this(watchDirectory, file -> true, onChangeConsumer);
    }

    public FileWatcher(File watchDirectory,
            SerializablePredicate<File> fileFilter,
            SerializableConsumer<File> onChangeConsumer) {
        this(watchDirectory, fileFilter,
                new DefaultFileListener(onChangeConsumer));
    }

    public FileWatcher(File watchDirectory,
            SerializablePredicate<File> fileFilter,
            FileAlterationListener listener) {
        Objects.requireNonNull(watchDirectory,
                "Watch directory cannot be empty");
        Objects.requireNonNull(fileFilter, "File filter cannot be empty");
        Objects.requireNonNull(listener,
                "File alteration listener cannot be empty");
        FileAlterationObserver observer = new FileAlterationObserver(
                watchDirectory, fileFilter::test);
        observer.addListener(listener);
        monitor = new FileAlterationMonitor(timeout);
        monitor.addObserver(observer);
    }

    public void start() throws Exception {
        monitor.start();
    }

    public void stop() throws Exception {
        monitor.stop();
    }

    public void stop(long stopInterval) throws Exception {
        monitor.stop(stopInterval);
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

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
