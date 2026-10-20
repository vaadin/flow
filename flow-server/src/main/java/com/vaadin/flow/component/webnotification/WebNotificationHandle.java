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
package com.vaadin.flow.component.webnotification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.WebNotificationErrorReader;
import com.vaadin.flow.component.webnotification.WebNotification.WebNotificationJs;
import com.vaadin.flow.component.webnotification.WebNotificationError.Reason;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.shared.Registration;

/**
 * A notification shown with {@link WebNotification#show}. Use it to react when
 * the user clicks the notification, to learn when it is gone, and to close it
 * from the server:
 *
 * <pre>{@code
 * WebNotificationHandle notification = WebNotification.show("Report ready");
 * notification.addClickListener(() -> ui.navigate(ReportView.class));
 * }</pre>
 *
 * The browser reports what happens to the notification asynchronously, so a
 * listener added right after {@code show} never misses an event. Every listener
 * runs on the UI thread.
 * <p>
 * The handle reports what happens to the notification until it is closed or
 * fails to show. A notification the user neither clicks nor dismisses can stay
 * in the operating system's notification center for a long time, and the
 * browser does not always report when it is gone, so a UI tracks at most the
 * {@value #MAX_TRACKED_PER_UI} most recently shown notifications: showing
 * another one stops tracking the oldest. A handle also stops being tracked when
 * its UI is closed, for example on a page reload. Either way its close
 * listeners run, and after that no listener runs again and {@link #close()}
 * does nothing, since the browser no longer keeps the notification for closing
 * either. Call {@link #close()} when a notification is no longer relevant, or
 * use a {@linkplain WebNotificationOptions#tag() tag} so the next notification
 * replaces it.
 */
public final class WebNotificationHandle implements Serializable {

    /**
     * How many notifications a UI tracks at most.
     */
    static final int MAX_TRACKED_PER_UI = 50;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebNotificationHandle.class);

    private final UI ui;
    private final String key = UUID.randomUUID().toString();
    private final List<SerializableRunnable> clickListeners = new ArrayList<>();
    private final List<SerializableRunnable> closeListeners = new ArrayList<>();
    private final List<SerializableConsumer<WebNotificationError>> errorListeners = new ArrayList<>();
    // Whether events from the browser still reach the listeners and close()
    // can still close the notification
    private boolean tracked = true;

    WebNotificationHandle(UI ui, String title, WebNotificationOptions options) {
        this.ui = ui;
        TrackedHandles tracker = TrackedHandles.of(ui);
        tracker.add(this);
        ui.getElement().executeJs(WebNotificationJs.class).show(title, options,
                key, tracker.channel, ui.getInternals().getAppId())
                .then(ignored -> {
                }, err -> {
                    LOGGER.debug("Client-side webNotification.show failed: {}",
                            err);
                    fail(new WebNotificationError(Reason.UNKNOWN,
                            "Client-side webNotification bridge failure: "
                                    + err));
                });
    }

    /**
     * Adds a listener that runs when the user clicks the notification. The
     * browser brings the tab to the front and closes the notification before
     * the listener runs, so the listener typically navigates to whatever the
     * notification was about.
     *
     * @param listener
     *            the listener, not {@code null}
     * @return a registration for removing the listener
     */
    public Registration addClickListener(SerializableRunnable listener) {
        return add(clickListeners, listener);
    }

    /**
     * Adds a listener that runs when the notification is gone: the user
     * dismissed it or clicked it, {@link #close()} closed it, or a notification
     * with the same {@linkplain WebNotificationOptions#tag() tag} replaced it.
     * Also runs when the handle stops tracking the notification, because the UI
     * is closed or has shown too many newer notifications; see the class
     * documentation. Not called when the notification failed to show; see
     * {@link #addErrorListener(SerializableConsumer)}.
     *
     * @param listener
     *            the listener, not {@code null}
     * @return a registration for removing the listener
     */
    public Registration addCloseListener(SerializableRunnable listener) {
        return add(closeListeners, listener);
    }

    /**
     * Adds a listener that runs when the browser did not show the notification,
     * for example because permission has not been granted. Use it to fall back
     * to an in-page message.
     *
     * @param listener
     *            the listener receiving why the notification was not shown, not
     *            {@code null}
     * @return a registration for removing the listener
     */
    public Registration addErrorListener(
            SerializableConsumer<WebNotificationError> listener) {
        return add(errorListeners, listener);
    }

    /**
     * Closes the notification if it is still visible. The close listeners run
     * once the browser confirms. Calling this when the notification is already
     * gone, or when the handle no longer tracks it, does nothing.
     * <p>
     * Like any change to the UI, the call must hold the session lock, so call
     * it inside {@code ui.access(...)} from a background thread.
     */
    public void close() {
        if (!tracked) {
            return;
        }
        ui.getElement().executeJs(WebNotificationJs.class).close(key)
                .then(ignored -> {
                }, err -> LOGGER.debug(
                        "Client-side webNotification.close failed: {}", err));
    }

    private static <T> Registration add(List<T> listeners, T listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void dispatch(ArrayNode args) {
        String type = args.get(0).asString();
        switch (type) {
        case "click" ->
            List.copyOf(clickListeners).forEach(SerializableRunnable::run);
        case "close" -> {
            stopTracking();
        }
        case "error" -> fail(WebNotificationErrorReader.read(args));
        default -> LOGGER
                .debug("Unknown web notification event from client: {}", type);
        }
    }

    private void fail(WebNotificationError error) {
        if (!tracked) {
            return;
        }
        tracked = false;
        TrackedHandles.of(ui).remove(this);
        List.copyOf(errorListeners).forEach(listener -> listener.accept(error));
    }

    /**
     * Stops reporting events to the listeners, running the close listeners for
     * the last time.
     */
    private void stopTracking() {
        if (!tracked) {
            return;
        }
        tracked = false;
        TrackedHandles.of(ui).remove(this);
        List.copyOf(closeListeners).forEach(SerializableRunnable::run);
    }

    /**
     * The handles of a UI that are still tracked, in the order they were shown,
     * and the channel the browser reports their events through. A single
     * channel keyed by handle lets an event for a handle that is no longer
     * tracked be ignored quietly.
     */
    private static final class TrackedHandles implements Serializable {

        private final LinkedHashMap<String, WebNotificationHandle> handles = new LinkedHashMap<>();
        private final ReturnChannelRegistration channel;

        private TrackedHandles(UI ui) {
            channel = ui.getElement().getNode()
                    .getFeature(ReturnChannelMap.class)
                    .registerChannel(this::dispatch);
        }

        private static TrackedHandles of(UI ui) {
            TrackedHandles tracker = ComponentUtil.getData(ui,
                    TrackedHandles.class);
            if (tracker == null) {
                TrackedHandles created = new TrackedHandles(ui);
                ComponentUtil.setData(ui, TrackedHandles.class, created);
                // A closed UI, also the one a page reload with
                // @PreserveOnRefresh replaces, never hears from its
                // notifications again
                ui.addDetachListener(
                        event -> List.copyOf(created.handles.values())
                                .forEach(WebNotificationHandle::stopTracking));
                tracker = created;
            }
            return tracker;
        }

        private void add(WebNotificationHandle handle) {
            handles.put(handle.key, handle);
            if (handles.size() > MAX_TRACKED_PER_UI) {
                // The browser has stopped keeping it for closing as well
                handles.values().iterator().next().stopTracking();
            }
        }

        private void remove(WebNotificationHandle handle) {
            handles.remove(handle.key);
        }

        // The browser reports (key, type, reason, message)
        private void dispatch(ArrayNode args) {
            String key = args.get(0).asString();
            WebNotificationHandle handle = handles.get(key);
            if (handle == null) {
                LOGGER.debug(
                        "Ignoring an event for a web notification that is no"
                                + " longer tracked: {}",
                        args);
                return;
            }
            ArrayNode event = args.deepCopy();
            event.remove(0);
            handle.dispatch(event);
        }
    }
}
