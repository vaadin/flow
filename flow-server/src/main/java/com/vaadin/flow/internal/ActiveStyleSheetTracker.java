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

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Tracks the set of active stylesheet URLs used by the running application
 * during development time.
 * <p>
 * The tracker keeps a per-session set of stylesheet URLs that are currently
 * applied via {@code @StyleSheet} and a global set for AppShell stylesheets
 * that are applied to all pages.
 * <p>
 * The data is stored in the {@link VaadinContext} and can be retrieved from any
 * module using {@link #get(VaadinService)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ActiveStyleSheetTracker implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<VaadinSession, Set<String>> sessionToUrls = new ConcurrentHashMap<>();
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
     * Register that the given stylesheet URL is active in the provided session.
     *
     * @param session
     *            the {@link VaadinSession}
     * @param url
     *            the stylesheet URL (as passed to addStyleSheet)
     */
    public void registerAdded(VaadinSession session, String url) {
        if (session == null || url == null || url.isBlank()) {
            return;
        }
        sessionToUrls
                .computeIfAbsent(session, s -> ConcurrentHashMap.newKeySet())
                .add(url);
    }

    /**
     * Register that the given stylesheet URL is no longer active in the
     * provided session.
     *
     * @param session
     *            the {@link VaadinSession}
     * @param url
     *            the stylesheet URL
     */
    public void registerRemoved(VaadinSession session, String url) {
        if (session == null || url == null || url.isBlank()) {
            return;
        }
        Set<String> urls = sessionToUrls.get(session);
        if (urls != null) {
            urls.remove(url);
            if (urls.isEmpty()) {
                sessionToUrls.remove(session);
            }
        }
    }

    /**
     * Replaces the set of AppShell stylesheet URLs that are applied globally.
     *
     * @param urls
     *            the new set of app shell stylesheet URLs
     */
    public void setAppShellUrls(Collection<String> urls) {
        appShellUrls.clear();
        if (urls != null) {
            appShellUrls.addAll(urls);
        }
    }

    /**
     * Returns all currently active stylesheet URLs across all sessions,
     * including AppShell URLs.
     *
     * @return a new set containing all active stylesheet URLs
     */
    public Set<String> getActiveUrls() {
        Set<String> all = ConcurrentHashMap.newKeySet();
        all.addAll(appShellUrls);
        sessionToUrls.values().forEach(all::addAll);
        return all;
    }

    /**
     * Returns active stylesheet URLs for the given session, merged with
     * AppShell URLs.
     *
     * @param session
     *            the session
     * @return a new set of active stylesheet URLs for the session
     */
    public Set<String> getActiveUrls(VaadinSession session) {
        if (session == null) {
            return Collections.unmodifiableSet(appShellUrls);
        }
        Set<String> urls = sessionToUrls.get(session);
        if (urls == null || urls.isEmpty()) {
            return Collections.unmodifiableSet(appShellUrls);
        }
        return urls.stream().collect(
                Collectors.collectingAndThen(Collectors.toSet(), set -> {
                    set.addAll(appShellUrls);
                    return set;
                }));
    }
}
