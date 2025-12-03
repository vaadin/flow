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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.ActiveStyleSheetTracker;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Watches source public resource folders for CSS changes and pushes updates to
 * the browser via the debug window connection.
 * <p>
 * Watched source roots map to public static resources in the running
 * application, such as:
 * <ul>
 * <li>src/main/resources/META-INF/resources</li>
 * <li>src/main/resources/resources</li>
 * <li>src/main/resources/static</li>
 * <li>src/main/resources/public</li>
 * <li>src/main/webapp</li>
 * </ul>
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class PublicResourcesLiveUpdater implements Closeable {

    private final List<FileWatcher> watchers = new ArrayList<>();
    private final List<File> roots = new ArrayList<>();
    private final VaadinContext context;
    private final PublicStyleSheetBundler bundler;

    /**
     * Starts watching the given list of source folders for CSS changes.
     *
     * @param roots
     *            the list of source folders to watch; non-existing ones are
     *            ignored
     * @param context
     *            the current Vaadin context
     */
    public PublicResourcesLiveUpdater(List<String> roots,
            VaadinContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        this.context = context;
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);

        for (String root : roots) {
            File rootLocation = new File(root);
            if (rootLocation.exists() && rootLocation.isDirectory()) {
                this.roots.add(rootLocation);
            }
        }
        this.bundler = PublicStyleSheetBundler.forResourceLocations(this.roots);
        if (liveReload.isEmpty()) {
            getLogger().error(
                    "Browser live reload is not available. Unable to watch public resources for changes");
            return;
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

    private FileWatcher getFileWatcher(File root, BrowserLiveReload liveReload)
            throws IOException {
        FileWatcher watcher = new FileWatcher(file -> {
            if (file.isDirectory()) {
                return;
            }
            if (isTempFile(file)) {
                // temp file created by IDE, ignore
                return;
            }
            if (!file.getName().endsWith(".css")) {
                liveReload.reload();
                return;
            }
            try {
                // When any css file under public roots changes, rebundle all
                // active @StyleSheet URLs
                Set<String> activeUrls = ActiveStyleSheetTracker.get(context)
                        .getActiveUrls();
                if (activeUrls.isEmpty()) {
                    return;
                }
                for (String url : activeUrls) {
                    if (isVaadinThemeUrl(url)) {
                        // ignore Aura and Lumo urls
                        continue;
                    }
                    String normalized = PublicStyleSheetBundler
                            .normalizeUrl(url);
                    String contextPath = getContextPath();
                    String content = bundler.bundle(url, contextPath)
                            .orElse(null);
                    String path = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                            + normalized;
                    liveReload.update(path, content);
                    getLogger().debug("Pushed bundled stylesheet update for {}",
                            path);

                }
            } catch (Exception e) {
                getLogger().error(
                        "Unable to perform hot update for CSS change under root {}, fall back to page reload",
                        root, e);
                try {
                    liveReload.reload();
                } catch (Exception ignore) {
                    getLogger().error("Failed to reload resource changes", e);
                }
            }
        }, root);
        watcher.start();
        return watcher;
    }

    private boolean isVaadinThemeUrl(String url) {
        url = FrontendUtils.getUnixPath(new File(url).toPath());
        return url.contains("lumo/lumo.css") || url.contains("aura/aura.css");
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

    private boolean isTempFile(File file) {
        String name = file.getName();
        return name.startsWith("~") || name.endsWith("~");
    }

    private String getContextPath() {
        String contextPath = "";
        if (context instanceof VaadinServletContext) {
            contextPath = ((VaadinServletContext) context).getContext()
                    .getContextPath();
        }
        return contextPath;
    }
}
