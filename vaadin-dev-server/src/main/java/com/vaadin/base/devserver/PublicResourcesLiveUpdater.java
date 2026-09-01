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
package com.vaadin.base.devserver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.ActiveStyleSheetTracker;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
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
 * 
 * @since 25.0
 */
public class PublicResourcesLiveUpdater implements Closeable {
    private final static Pattern THEME_URLS_PATTERN = Pattern
            .compile("^(.*/)?(lumo|aura)/.+\\.css$");
    private final List<FileWatcher> watchers = new ArrayList<>();
    private final List<File> roots = new ArrayList<>();
    private final VaadinContext context;
    private final PublicStyleSheetBundler bundler;
    private final ResourceProvider resourceProvider;

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
        Lookup lookup = context.getAttribute(Lookup.class);
        this.resourceProvider = lookup != null
                ? lookup.lookup(ResourceProvider.class)
                : null;
        // Published before the live-reload check below, because a tool that
        // suspends this watcher still needs the bundler and the roots it
        // resolved. Whether a browser is connected is asked again at push time.
        context.setAttribute(PublicResourcesLiveUpdater.class, this);
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

    /**
     * Stops this watcher from pushing CSS changes for the given context.
     * <p>
     * For a tool that owns the edit-to-running-app loop, watching on
     * <em>save</em> is the wrong trigger: the loop decides when a change goes
     * live, and a second watcher pushing on its own makes "what is the state of
     * my last change?" unanswerable. Such a tool suspends this and performs the
     * resource leg itself.
     * <p>
     * A context attribute rather than a field, so it can be set before or after
     * the watcher is created - the check happens when a file changes.
     *
     * @param context
     *            the current Vaadin context
     */
    public static void suspend(VaadinContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        context.setAttribute(Suspended.class, new Suspended());
        LoggerFactory.getLogger(PublicResourcesLiveUpdater.class).debug(
                "Public resource live updates suspended; another tool owns the resource leg");
    }

    private static boolean isSuspended(VaadinContext context) {
        return context.getAttribute(Suspended.class) != null;
    }

    /**
     * Re-bundles every active {@code @StyleSheet} URL and pushes the result to
     * the browser, which is what the watcher does when a public CSS file is
     * saved.
     * <p>
     * Separate from the watcher so a tool that owns the apply loop can perform
     * exactly the same update at the moment it decides a change goes live,
     * rather than reimplementing the bundling and the URL. See
     * {@link #suspend(VaadinContext)}, and {@link ThemeLiveUpdater#push} for
     * the theme leg's equivalent.
     * <p>
     * Every active URL rather than only the file that changed, because an
     * {@code @import} means the stylesheet the browser knows is not the file
     * that was edited - and the URL the browser knows it by is what the update
     * has to be keyed on.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param context
     *            the current Vaadin context
     * @param extraRoots
     *            public resource roots to resolve against in addition to this
     *            project's own, for a caller that knows of roots this watcher
     *            does not - a reactor sibling's, say. Empty for the watcher
     *            itself.
     * @return how many stylesheets reached the browser
     */
    public static int push(VaadinContext context, List<File> extraRoots) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(extraRoots, "extraRoots cannot be null");
        PublicResourcesLiveUpdater updater = context
                .getAttribute(PublicResourcesLiveUpdater.class);
        if (updater == null) {
            getStaticLogger().debug(
                    "No public resource updater for this context; nothing to push");
            return 0;
        }
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);
        if (liveReload.isEmpty()) {
            getStaticLogger().debug(
                    "Browser live reload is not available; nothing to push");
            return 0;
        }
        try {
            return updater.pushActiveStyleSheets(liveReload.get(),
                    updater.bundlerFor(extraRoots));
        } catch (Exception e) {
            getStaticLogger().error(
                    "Unable to push public stylesheet updates; the caller decides whether to reload",
                    e);
            return 0;
        }
    }

    /**
     * The bundler to resolve this push against.
     * <p>
     * This project's own roots first, so a file that exists in both resolves to
     * the application's copy - the same precedence the classpath gives it.
     */
    private PublicStyleSheetBundler bundlerFor(List<File> extraRoots) {
        if (extraRoots.isEmpty()) {
            return bundler;
        }
        List<File> combined = new ArrayList<>(roots);
        extraRoots.stream().filter(root -> !combined.contains(root))
                .forEach(combined::add);
        return PublicStyleSheetBundler.forResourceLocations(combined);
    }

    /**
     * Pushes the current content of every active {@code @StyleSheet} URL this
     * project owns, keyed by the URL the browser knows it as.
     * <p>
     * The {@code context://} prefix is part of that key, not decoration: the
     * debug window strips it unconditionally before matching the stylesheet
     * already on the page, so a path sent without it does not identify anything
     * and the stylesheet the page bootstrapped with is left in place - which
     * shows up as a removed CSS rule that goes on applying.
     *
     * @param liveReload
     *            the connection to push over
     * @param bundler
     *            the bundler to resolve the entry files with
     * @return how many stylesheets were pushed
     */
    private int pushActiveStyleSheets(BrowserLiveReload liveReload,
            PublicStyleSheetBundler bundler) {
        // When any css file under public roots changes, rebundle all
        // active @StyleSheet URLs
        Set<String> activeUrls = ActiveStyleSheetTracker.get(context)
                .getActiveUrls();
        int pushed = 0;
        for (String url : activeUrls) {
            if (isVaadinThemeUrl(url) || isExternalUrl(url)) {
                // ignore external urls, and Aura and Lumo urls
                continue;
            }
            String normalized = PublicStyleSheetBundler.normalizeUrl(url);
            String contextPath = getContextPath();
            Optional<String> content = bundler.bundle(url, contextPath);
            if (content.isEmpty() && isClasspathResource(normalized)) {
                // Resource exists on classpath (e.g. from an addon
                // JAR) but not in local source roots — leave it
                // untouched
                continue;
            }
            String path = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                    + normalized;
            // Null content for a stylesheet that is gone, which is what takes
            // it off the page rather than leaving it there stale.
            liveReload.update(path, content.orElse(null));
            getLogger().debug("Pushed bundled stylesheet update for {}", path);
            pushed++;
        }
        return pushed;
    }

    private static Logger getStaticLogger() {
        return LoggerFactory.getLogger(PublicResourcesLiveUpdater.class);
    }

    /** Marker for {@link #suspend(VaadinContext)}. */
    private static final class Suspended implements java.io.Serializable {
    }

    private FileWatcher getFileWatcher(File root, BrowserLiveReload liveReload)
            throws IOException {
        FileWatcher watcher = new FileWatcher(file -> {
            if (isSuspended(context)) {
                return;
            }
            if (file.isDirectory()) {
                return;
            }
            if (isTempFile(file)) {
                // temp file created by IDE, ignore
                return;
            }
            if (!file.getName().endsWith(".css")) {
                // non-CSS resources like images/fonts need IDE copying to
                // output dir, so full reload is not reliable, ignore
                return;
            }
            try {
                pushActiveStyleSheets(liveReload, bundler);
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

    private boolean isExternalUrl(String url) {
        if (url.startsWith(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)
                || url.startsWith(ApplicationConstants.BASE_PROTOCOL_PREFIX)) {
            return false;
        }
        try {
            return URI.create(url).isAbsolute();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean isVaadinThemeUrl(String url) {
        // all known urls from Aura and Lumo classes
        return THEME_URLS_PATTERN
                .matcher(PublicStyleSheetBundler.toUnixSeparators(url))
                .matches();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    public void close() throws IOException {
        if (context.getAttribute(PublicResourcesLiveUpdater.class) == this) {
            context.removeAttribute(PublicResourcesLiveUpdater.class);
        }
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

    private boolean isClasspathResource(String normalizedPath) {
        if (resourceProvider == null) {
            return false;
        }
        return resourceProvider.getApplicationResource(
                "META-INF/resources/" + normalizedPath) != null;
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
