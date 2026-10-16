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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.UI;
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
 * The handle stays connected to the browser until the notification is closed or
 * fails to show. A notification the user neither clicks nor dismisses can stay
 * in the operating system's notification center for a long time; call
 * {@link #close()} when it is no longer relevant, or use a
 * {@linkplain WebNotificationOptions#tag() tag} so the next notification
 * replaces it.
 */
public final class WebNotificationHandle implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebNotificationHandle.class);

    private final UI ui;
    private final String key = UUID.randomUUID().toString();
    private final List<SerializableRunnable> clickListeners = new ArrayList<>();
    private final List<SerializableRunnable> closeListeners = new ArrayList<>();
    private final List<SerializableConsumer<WebNotificationError>> errorListeners = new ArrayList<>();
    private @Nullable ReturnChannelRegistration channel;

    WebNotificationHandle(UI ui, String title, WebNotificationOptions options) {
        this.ui = ui;
        ReturnChannelRegistration registration = ui.getElement().getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(this::dispatch);
        channel = registration;
        ui.getElement().executeJs(WebNotificationJs.class)
                .show(title, options, key, registration).then(ignored -> {
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
     * Not called when the notification failed to show; see
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
     * gone does nothing.
     * <p>
     * Like any change to the UI, the call must hold the session lock, so call
     * it inside {@code ui.access(...)} from a background thread.
     */
    public void close() {
        if (channel == null) {
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
            disconnect();
            List.copyOf(closeListeners).forEach(SerializableRunnable::run);
        }
        case "error" -> fail(new WebNotificationError(parseReason(args.get(1)),
                textOrEmpty(args.get(2))));
        default -> LOGGER
                .debug("Unknown web notification event from client: {}", type);
        }
    }

    private void fail(WebNotificationError error) {
        disconnect();
        List.copyOf(errorListeners).forEach(listener -> listener.accept(error));
    }

    private void disconnect() {
        if (channel != null) {
            channel.remove();
            channel = null;
        }
    }

    private static Reason parseReason(@Nullable JsonNode value) {
        try {
            return value == null ? Reason.UNKNOWN
                    : Reason.valueOf(value.asString());
        } catch (IllegalArgumentException e) {
            return Reason.UNKNOWN;
        }
    }

    private static String textOrEmpty(@Nullable JsonNode value) {
        return value == null || value.isNull() ? "" : value.asString();
    }
}
