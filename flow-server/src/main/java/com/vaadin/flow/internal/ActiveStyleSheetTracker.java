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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

/**
 * Tracks the set of all active stylesheets used by the running application
 * during development time.
 * <p>
 * The tracker keeps a per-session set of stylesheet URLs that are currently
 * applied via {@code @StyleSheet} and a global set for AppShell stylesheets
 * that are applied to all pages.
 * <p>
 * Per-session tracking is used for component-based annotations and to avoid
 * accidentally adding stylesheets to those UIs not using a given component,
 * when a new stylesheet is added and hot-reloaded.
 * <p>
 * The data is stored in the {@link VaadinContext} and can be retrieved from any
 * module using {@link #get(VaadinService)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ActiveStyleSheetTracker implements Serializable {

    private final Set<String> componentUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> appShellUrls = ConcurrentHashMap.newKeySet();

    private ActiveStyleSheetTracker() {
    }

    /**
     * Returns the tracker instance stored in the given service context,
     * creating it if necessary.
     *
     * @param service
     *            the active {@link VaadinService}
     * @return the tracker instance, never null
     */
    public static ActiveStyleSheetTracker get(VaadinService service) {
        Objects.requireNonNull(service, "service cannot be null");
        VaadinContext context = service.getContext();
        return context.getAttribute(ActiveStyleSheetTracker.class,
                ActiveStyleSheetTracker::new);
    }

    /**
     * Returns the tracker instance stored in the given {@link VaadinContext},
     * creating it if necessary.
     *
     * @param context
     *            the {@link VaadinContext}
     * @return the tracker instance, never null
     */
    public static ActiveStyleSheetTracker get(VaadinContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        return context.getAttribute(ActiveStyleSheetTracker.class,
                ActiveStyleSheetTracker::new);
    }

    /**
     * Register that the given stylesheet URL is active on the page and eligible
     * for hot-reload.
     *
     * @param url
     *            the stylesheet URL to be hot-reloaded
     */
    public void trackAddForComponent(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        componentUrls.add(url);
    }

    /**
     * Register that the given stylesheet URL is no longer active on the page
     * and should not be hot-reloaded.
     *
     * @param url
     *            the stylesheet URL to be skipped during hot-reload
     */
    public void trackRemoveForComponent(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        componentUrls.remove(url);
    }

    /**
     * Replaces the set of active AppShell stylesheet URLs.
     *
     * @param urls
     *            the new set of app shell stylesheet URLs to be hot-reloaded
     */
    public void trackForAppShell(Collection<String> urls) {
        appShellUrls.clear();
        if (urls != null) {
            appShellUrls.addAll(urls);
        }
    }

    /**
     * Returns all currently active stylesheet URLs, including AppShell URLs and
     * components.
     *
     * @return a new set containing all active stylesheet URLs to be
     *         hot-reloaded
     */
    public Set<String> getActiveUrls() {
        Set<String> all = ConcurrentHashMap.newKeySet();
        all.addAll(appShellUrls);
        all.addAll(componentUrls);
        return all;
    }
}
