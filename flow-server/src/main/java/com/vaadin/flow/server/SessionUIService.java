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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.component.UI;

/**
 * Keeps track of the {@link UI} instances of every {@link VaadinSession}
 * outside of the session itself.
 * <p>
 * The storage is a {@link ConcurrentHashMap} keyed by the session (identity,
 * since {@link VaadinSession} does not override {@code equals}) with a
 * {@link CopyOnWriteArrayList} of UIs as value. Reads are therefore lock-free
 * and iteration always works on a consistent snapshot, which makes it possible
 * to enumerate UIs without holding the session lock (e.g. for broadcasting or
 * monitoring). Mutating a UI still requires its session lock as usual.
 * <p>
 * Writes are expected to happen through {@link VaadinSession#addUI(UI)} and
 * {@link VaadinSession#removeUI(UI)}, i.e. while the session lock is held.
 * <p>
 * One instance is owned by each {@link VaadinService}, see
 * {@link VaadinService#getSessionUIService()}.
 */
public class SessionUIService {

    private final ConcurrentHashMap<VaadinSession, CopyOnWriteArrayList<UI>> uisBySession = new ConcurrentHashMap<>();

    /**
     * Gets the UIs of the given session.
     *
     * @param session
     *            the session, not {@code null}
     * @return an unmodifiable, thread-safe view of the session's UIs, never
     *         {@code null}
     */
    public List<UI> getUIs(VaadinSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        CopyOnWriteArrayList<UI> uis = uisBySession.get(session);
        return uis == null ? Collections.emptyList()
                : Collections.unmodifiableList(uis);
    }

    /**
     * Gets the UI with the given id from the given session.
     *
     * @param session
     *            the session, not {@code null}
     * @param uiId
     *            the UI id
     * @return the UI, or {@code null} if not found
     */
    public UI getUIById(VaadinSession session, int uiId) {
        for (UI ui : getUIs(session)) {
            if (ui.getUIId() == uiId) {
                return ui;
            }
        }
        return null;
    }

    /**
     * Registers a UI for the given session. An already registered UI with the
     * same id is replaced.
     *
     * @param session
     *            the session, not {@code null}
     * @param ui
     *            the UI, not {@code null}
     */
    public void addUI(VaadinSession session, UI ui) {
        Objects.requireNonNull(session, "Session cannot be null");
        Objects.requireNonNull(ui, "UI cannot be null");
        uisBySession.compute(session, (key, uis) -> {
            CopyOnWriteArrayList<UI> list = uis == null
                    ? new CopyOnWriteArrayList<>()
                    : uis;
            list.removeIf(existing -> existing.getUIId() == ui.getUIId());
            list.add(ui);
            return list;
        });
    }

    /**
     * Removes a UI from the given session.
     *
     * @param session
     *            the session, not {@code null}
     * @param ui
     *            the UI, not {@code null}
     * @return {@code true} if the UI was registered
     */
    public boolean removeUI(VaadinSession session, UI ui) {
        Objects.requireNonNull(session, "Session cannot be null");
        Objects.requireNonNull(ui, "UI cannot be null");
        CopyOnWriteArrayList<UI> uis = uisBySession.get(session);
        return uis != null
                && uis.removeIf(existing -> existing.getUIId() == ui.getUIId());
    }

    /**
     * Drops all bookkeeping for the given session. Must be called when the
     * session is destroyed, otherwise the session and its UIs leak.
     *
     * @param session
     *            the session, not {@code null}
     * @return the UIs that were still registered, never {@code null}
     */
    public List<UI> removeSession(VaadinSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        CopyOnWriteArrayList<UI> uis = uisBySession.remove(session);
        return uis == null ? Collections.emptyList()
                : Collections.unmodifiableList(uis);
    }

    /**
     * Gets all sessions that currently have registered UIs.
     *
     * @return an unmodifiable, weakly consistent view of the sessions
     */
    public Set<VaadinSession> getSessions() {
        return Collections.unmodifiableSet(uisBySession.keySet());
    }

    /**
     * Removes all registered sessions and UIs.
     */
    public void clear() {
        uisBySession.clear();
    }
}
