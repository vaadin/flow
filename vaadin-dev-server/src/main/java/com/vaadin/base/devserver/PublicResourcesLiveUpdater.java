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

import com.vaadin.flow.server.VaadinService;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Watches source public resource folders for CSS changes and pushes updates to
 * the browser via the debug window connection.
 * <p>
 * Watched source roots map to public static resources in the running
 * application, such as:
 * <ul>
 *   <li>src/main/resources/META-INF/resources</li>
 *   <li>src/main/resources/resources</li>
 *   <li>src/main/resources/static</li>
 *   <li>src/main/resources/public</li>
 *   <li>src/main/webapp</li>
 * </ul>
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class PublicResourcesLiveUpdater implements Closeable {

    private final List<FileWatcher> watchers = new ArrayList<>();
    private final List<File> roots = new ArrayList<>();

    /**
     * Starts watching the given list of source folders for CSS changes.
     *
     * @param roots
     *            the list of source folders to watch; non-existing ones are
     *            ignored
     * @param context
     *            the current Vaadin context
     */
    public PublicResourcesLiveUpdater(List<String> roots, VaadinContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);
        if (liveReload.isEmpty()) {
            getLogger().error(
                    "Browser live reload is not available. Unable to watch public resources for changes");
            return;
        }

        for (String root : roots) {
            File rootLocation = new File(root);
            if (rootLocation.exists() && rootLocation.isDirectory()) {
                this.roots.add(rootLocation);
            }
        }

        try {
            for (File root : this.roots) {
                FileWatcher watcher = getFileWatcher(root, liveReload.get());
                watchers.add(watcher);
                getLogger().debug("Watching {} for public CSS changes", root);
            }
        } catch (IOException e) {
            getLogger().error(
                    "Unable to watch public resources for changes under {}",
                    this.roots, e);
        }
    }

    private FileWatcher getFileWatcher(File root, BrowserLiveReload liveReload) throws IOException {
        FileWatcher watcher = new FileWatcher(file -> {
            if (file.isDirectory()) {
                return;
            }
            if (!file.getName().endsWith(".css")) {
                return;
            }
            try {
                String contextRelativePath = toContextRelativePath(root,
                        file);
                if (contextRelativePath != null) {
                    String path = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                            + contextRelativePath;
                    liveReload.update(path, null);
                    getLogger().debug(
                            "Updated stylesheet {} via live reload",
                            path);
                }
            } catch (Exception e) {
                getLogger().error(
                        "Unable to perform hot update of {} in root {}",
                        file, root, e);
            }
        }, root);
        watcher.start();
        return watcher;
    }

    private String toContextRelativePath(File root, File file) {
        try {
            Path rootPath = root.getCanonicalFile().toPath();
            Path filePath = file.getCanonicalFile().toPath();
            if (!filePath.startsWith(rootPath)) {
                return null;
            }
            Path relative = rootPath.relativize(filePath);
            String unix = FrontendUtils.getUnixPath(relative);
            // Ensure no leading slash
            if (unix.startsWith("/")) {
                unix = unix.substring(1);
            }
            // Ensure no leading './'
            if (unix.startsWith("./")) {
                unix = unix.substring(2);
            }
            return unix;
        } catch (IOException e) {
            return null;
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    public void close() throws IOException {
        for (FileWatcher watcher : watchers) {
            try {
                watcher.stop();
            } catch (Exception e) {
                getLogger().error("Failed to stop watcher {}", watcher, e);
            }
        }
        watchers.clear();
        roots.clear();
    }
}
