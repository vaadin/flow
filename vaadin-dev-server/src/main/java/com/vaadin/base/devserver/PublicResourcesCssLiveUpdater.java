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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;

/**
 * Watches given resourcesFolder for CSS file changes and performs hot CSS
 * updates via BrowserLiveReload.
 */
class PublicResourcesCssLiveUpdater implements Closeable {

    private final File resourcesFolder;
    private final VaadinContext context;
    private FileWatcher watcher;

    PublicResourcesCssLiveUpdater(File resourcesFolder, VaadinContext context)
            throws IOException {
        this.resourcesFolder = resourcesFolder;
        this.context = context;
        initWatcher();
    }

    private void initWatcher() throws IOException {
        if (resourcesFolder == null || !resourcesFolder.isDirectory()) {
            getLogger().debug(
                    "Public resources folder {} not found, skipping watcher",
                    resourcesFolder);
            return;
        }
        var liveReloadOpt = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);
        if (liveReloadOpt.isEmpty()) {
            getLogger().debug(
                    "Browser live reload not available, skipping public resources watcher");
            return;
        }
        BrowserLiveReload liveReload = liveReloadOpt.get();
        watcher = new FileWatcher(changed -> {
            try {
                if (changed.isFile() && changed.getName().endsWith(".css")) {
                    // Path to be used from the browser: "/" + relative path
                    String rel = resourcesFolder.toPath()
                            .relativize(changed.toPath()).toString()
                            .replace(File.separatorChar, '/');
                    String browserPath = "/" + rel;
                    String contents = Files.readString(changed.toPath(),
                            StandardCharsets.UTF_8);
                    liveReload.update(browserPath, contents);
                }
            } catch (Exception e) {
                getLogger().error(
                        "Unable to perform hot update of public resource CSS "
                                + changed,
                        e);
                try {
                    liveReload.reload();
                } catch (Exception ignore) {
                    // no-op
                }
            }
        }, resourcesFolder);
        watcher.start();
        getLogger().debug("Watching {} for public CSS changes",
                resourcesFolder);
    }

    @Override
    public void close() throws IOException {
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
