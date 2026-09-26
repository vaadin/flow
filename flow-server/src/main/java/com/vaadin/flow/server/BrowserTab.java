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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;

/**
 * Holds state for one browser tab, shared by every {@link UI} that is loaded in
 * that tab during the session.
 * <p>
 * A new {@link UI} instance is created whenever the browser loads the
 * application page: on reload, when the user types a URL or opens a bookmark,
 * and when the back button returns from another page. Data stored in the UI, in
 * a view or in a layout is gone after that, unless the route chain is preserved
 * with {@code @PreserveOnRefresh}, which only covers a reload of the same URL.
 * Attributes stored in a browser tab stay available to all the UIs that are
 * loaded in the same tab, until the tab is destroyed:
 *
 * <pre>
 * BrowserTab tab = BrowserTab.getCurrent();
 * Booking booking = tab.getAttribute(Booking.class);
 * if (booking == null) {
 *     booking = new Booking();
 *     tab.setAttribute(Booking.class, booking);
 * }
 * </pre>
 *
 * The browser tab of a UI is available already in a {@code UIInitListener},
 * before any route target or layout of the UI is created.
 * <p>
 * The tab is identified by the {@code window.name} of the browser window, which
 * the Vaadin client sets to a random value when the application is loaded in a
 * window that does not have a name yet. The identification is therefore best
 * effort, with these caveats:
 * <ul>
 * <li>Duplicating a tab copies its window name in most browsers, so the
 * original and the duplicate share one browser tab on the server.</li>
 * <li>Browsers clear the window name when the tab navigates to another site, so
 * returning to the application from another site, for example after a redirect
 * from a payment provider or a login service, starts a new browser tab.</li>
 * <li>Application code that assigns {@code window.name}, or opens the
 * application with a named target, changes the identity of the tab.</li>
 * <li>When the window name of a UI is not known, which is the case for
 * applications that are only embedded as web components, the browser tab lives
 * only as long as that one UI.</li>
 * </ul>
 * <p>
 * The browser does not tell the server when a tab is closed. A closed tab can
 * not be told apart from a tab that navigated to another page and returns
 * later, or from a tab that is being reloaded. A browser tab is therefore
 * destroyed only when no UI of the tab has sent a heartbeat for the same time
 * that it takes to close an inactive UI (three missed heartbeats, see
 * {@link com.vaadin.flow.function.DeploymentConfiguration#getHeartbeatInterval()}),
 * or when the session is destroyed. The check runs at the end of a request to
 * the session, so a browser tab can stay around for longer. Use
 * {@link #addDestroyListener(Command)} to release resources held by the tab.
 * <p>
 * The browser tab is stored in the {@link VaadinSession} and all its methods
 * require the session to be locked, like the session attributes.
 */
@NullMarked
public final class BrowserTab implements Serializable {

    private final VaadinSession session;

    private final String id;

    private final Map<String, Object> attributes = new HashMap<>();

    private final List<Command> destroyListeners = new ArrayList<>();

    private long lastActiveTimestamp;

    private boolean destroyed;

    private BrowserTab(VaadinSession session, String id,
            long lastActiveTimestamp) {
        this.session = session;
        this.id = id;
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    /**
     * Gets the browser tab of the current UI.
     *
     * @return the browser tab of the current UI, or {@code null} if there is no
     *         current UI
     * @see UI#getCurrent()
     * @see #get(UI)
     */
    public static @Nullable BrowserTab getCurrent() {
        UI ui = UI.getCurrent();
        return ui == null ? null : get(ui);
    }

    /**
     * Gets the browser tab that the given UI is loaded in. Creates the browser
     * tab if none of the UIs in the tab has used it yet.
     *
     * @param ui
     *            the UI to get the browser tab for, not {@code null}
     * @return the browser tab of the UI, not {@code null}
     * @throws IllegalStateException
     *             if the UI does not belong to a session
     */
    public static BrowserTab get(UI ui) {
        Objects.requireNonNull(ui, "UI can not be null");
        VaadinSession session = ui.getSession();
        if (session == null) {
            throw new IllegalStateException(
                    "The UI does not belong to a session");
        }
        session.checkHasLock();
        BrowserTab tab = findTab(session, ui);
        if (tab == null) {
            String windowName = getWindowName(ui);
            tab = new BrowserTab(session,
                    windowName == null ? UUID.randomUUID().toString()
                            : windowName,
                    ui.getInternals().getLastHeartbeatTimestamp());
            getOrCreateRegistry(session).tabs.put(tab.id, tab);
        }
        ComponentUtil.setData(ui, BrowserTab.class, tab);
        return tab;
    }

    /**
     * Gets the id of this browser tab. The id is the {@code window.name} of the
     * browser window, or a random value if the window name of the UI that
     * created the browser tab was not known.
     *
     * @return the id of this browser tab, not {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the session this browser tab belongs to.
     *
     * @return the session of this browser tab, not {@code null}
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Stores a value in this browser tab. A {@code null} value removes the
     * attribute.
     * <p>
     * The version that takes a string uses the string as the name of the
     * attribute, the version that takes a class uses the fully qualified name
     * of the class.
     *
     * @param name
     *            the name of the attribute, not {@code null}
     * @param value
     *            the value to store, or {@code null} to remove the attribute
     * @throws IllegalStateException
     *             if this browser tab has been destroyed
     */
    public void setAttribute(String name, @Nullable Object value) {
        Objects.requireNonNull(name, "Attribute name can not be null");
        session.checkHasLock();
        if (destroyed) {
            throw new IllegalStateException(
                    "The browser tab has been destroyed");
        }
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    /**
     * Stores a value in this browser tab. A {@code null} value removes the
     * attribute.
     * <p>
     * The version that takes a class uses the fully qualified name of the class
     * as the name of the attribute, the version that takes a string uses the
     * string.
     *
     * @param <T>
     *            the type of the value
     * @param type
     *            the type that names the attribute, not {@code null}
     * @param value
     *            the value to store, or {@code null} to remove the attribute
     * @throws IllegalStateException
     *             if this browser tab has been destroyed
     */
    public <T> void setAttribute(Class<T> type, @Nullable T value) {
        Objects.requireNonNull(type, "Attribute type can not be null");
        setAttribute(type.getName(), value);
    }

    /**
     * Gets a value stored in this browser tab.
     * <p>
     * The version that takes a string looks up the attribute by the given name,
     * the version that takes a class looks it up by the fully qualified name of
     * the class.
     *
     * @param name
     *            the name of the attribute, not {@code null}
     * @return the stored value, or {@code null} if there is none
     */
    public @Nullable Object getAttribute(String name) {
        Objects.requireNonNull(name, "Attribute name can not be null");
        session.checkHasLock();
        return attributes.get(name);
    }

    /**
     * Gets a value stored in this browser tab.
     * <p>
     * The version that takes a class looks up the attribute by the fully
     * qualified name of the class, the version that takes a string looks it up
     * by the given name.
     *
     * @param <T>
     *            the type of the value
     * @param type
     *            the type that names the attribute, not {@code null}
     * @return the stored value, or {@code null} if there is none
     */
    public <T> @Nullable T getAttribute(Class<T> type) {
        Objects.requireNonNull(type, "Attribute type can not be null");
        return type.cast(getAttribute(type.getName()));
    }

    /**
     * Adds a listener that is run when this browser tab is destroyed, either
     * because none of its UIs has been active for the heartbeat timeout or
     * because the session is destroyed. The attributes of the browser tab are
     * still available while the listeners run.
     *
     * @param listener
     *            the listener to add, not {@code null}
     * @return a handle for removing the listener
     */
    public Registration addDestroyListener(Command listener) {
        Objects.requireNonNull(listener, "Listener can not be null");
        session.checkHasLock();
        return Registration.addAndRemove(destroyListeners, listener);
    }

    /**
     * Destroys the browser tabs of the given session that have no open UI and
     * whose UIs have not been active for the given time.
     *
     * @param session
     *            the session to clean up, locked
     * @param timeoutMillis
     *            the time without activity after which a browser tab is
     *            destroyed, or a negative number to keep browser tabs until the
     *            session is destroyed
     */
    static void destroyInactiveTabs(VaadinSession session, long timeoutMillis) {
        Registry registry = session.getAttribute(Registry.class);
        if (registry == null) {
            return;
        }
        Set<BrowserTab> openTabs = new HashSet<>();
        for (UI ui : session.getUIs()) {
            BrowserTab tab = findTab(session, ui);
            if (tab != null) {
                tab.lastActiveTimestamp = Math.max(tab.lastActiveTimestamp,
                        ui.getInternals().getLastHeartbeatTimestamp());
                if (!ui.isClosing()) {
                    openTabs.add(tab);
                }
            }
        }
        if (timeoutMillis < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (BrowserTab tab : new ArrayList<>(registry.tabs.values())) {
            if (!openTabs.contains(tab)
                    && now - tab.lastActiveTimestamp >= timeoutMillis) {
                registry.tabs.remove(tab.id);
                tab.destroy();
            }
        }
    }

    /**
     * Destroys all browser tabs of the given session.
     *
     * @param session
     *            the session that is being destroyed, locked
     */
    static void destroyAllTabs(VaadinSession session) {
        Registry registry = session.getAttribute(Registry.class);
        if (registry == null) {
            return;
        }
        session.setAttribute(Registry.class, null);
        registry.tabs.values().forEach(BrowserTab::destroy);
    }

    private void destroy() {
        destroyed = true;
        for (Command listener : new ArrayList<>(destroyListeners)) {
            try {
                listener.execute();
            } catch (Exception e) {
                session.getErrorHandler().error(new ErrorEvent(e));
            }
        }
        destroyListeners.clear();
        attributes.clear();
    }

    private static @Nullable BrowserTab findTab(VaadinSession session, UI ui) {
        BrowserTab tab = ComponentUtil.getData(ui, BrowserTab.class);
        if (tab != null && !tab.destroyed) {
            return tab;
        }
        Registry registry = session.getAttribute(Registry.class);
        String windowName = getWindowName(ui);
        if (registry == null || windowName == null) {
            return null;
        }
        return registry.tabs.get(windowName);
    }

    private static @Nullable String getWindowName(UI ui) {
        return ui.getInternals().getExtendedClientDetails().getWindowName();
    }

    private static Registry getOrCreateRegistry(VaadinSession session) {
        Registry registry = session.getAttribute(Registry.class);
        if (registry == null) {
            registry = new Registry();
            session.setAttribute(Registry.class, registry);
        }
        return registry;
    }

    private static class Registry implements Serializable {
        // Browser tabs by id, which is the window name for all browser tabs
        // that can be looked up by a new UI
        private final Map<String, BrowserTab> tabs = new LinkedHashMap<>();
    }
}
