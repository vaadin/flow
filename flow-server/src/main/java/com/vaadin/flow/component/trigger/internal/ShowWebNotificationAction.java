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
package com.vaadin.flow.component.trigger.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.webnotification.WebNotificationError;
import com.vaadin.flow.component.webnotification.WebNotificationError.Reason;
import com.vaadin.flow.component.webnotification.WebNotificationOptions;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * Shows a native operating system notification each time the bound trigger
 * fires. Showing a notification needs no user gesture, so any trigger works,
 * but the user must have granted permission (see
 * {@link RequestWebNotificationPermissionAction}).
 * <p>
 * The title is an {@link Action.Input}, so it can be a literal or a value read
 * on the client when the trigger fires, such as the value of a field.
 * <p>
 * Two construction modes: fire-and-forget, or with {@code onClick} and
 * {@code onError} callbacks that run on the UI thread when the user clicks one
 * of the notifications the action showed, or when the browser did not show one.
 * The callbacks are shared by every notification the action shows. They are
 * reported through the trigger's host, so they only run while the host is
 * attached, visible and enabled; a notification clicked after the user left the
 * view runs no callback.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ShowWebNotificationAction extends Action {

    private final Action.Input<String> title;
    private final WebNotificationOptions options;
    private final @Nullable SerializableRunnable onClick;
    private final @Nullable SerializableConsumer<WebNotificationError> onError;
    private final Map<StateNode, ReturnChannelRegistration> channelByNode = new IdentityHashMap<>();

    /**
     * Creates a fire-and-forget action: what happens to the notification is not
     * reported back to the server.
     *
     * @param title
     *            input producing the title of the notification, not
     *            {@code null}
     * @param options
     *            what the notification shows besides its title, and how it
     *            behaves, not {@code null}
     */
    public ShowWebNotificationAction(Action.Input<String> title,
            WebNotificationOptions options) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.options = Objects.requireNonNull(options,
                "options must not be null");
        this.onClick = null;
        this.onError = null;
    }

    /**
     * Creates an action that reports clicks on the notifications it shows, and
     * failures to show them, back to the server.
     *
     * @param title
     *            input producing the title of the notification, not
     *            {@code null}
     * @param options
     *            what the notification shows besides its title, and how it
     *            behaves, not {@code null}
     * @param onClick
     *            invoked on the UI thread when the user clicks a notification
     *            shown by this action, not {@code null}
     * @param onError
     *            invoked on the UI thread when the browser did not show a
     *            notification, not {@code null}
     */
    public ShowWebNotificationAction(Action.Input<String> title,
            WebNotificationOptions options, SerializableRunnable onClick,
            SerializableConsumer<WebNotificationError> onError) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.options = Objects.requireNonNull(options,
                "options must not be null");
        this.onClick = Objects.requireNonNull(onClick,
                "onClick must not be null");
        this.onError = Objects.requireNonNull(onError,
                "onError must not be null");
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // No key: the client generates one per notification, since only the
        // tag is used to close notifications shown by a trigger.
        if (onClick == null) {
            return JsFunction.of(
                    "window.Vaadin.Flow.webNotification.show($0(event), $1, null, null)",
                    title.toJs(trigger), options).withArguments("event");
        }
        return JsFunction.of(
                "window.Vaadin.Flow.webNotification.show($0(event), $1, null, $2)",
                title.toJs(trigger), options, channelFor(trigger))
                .withArguments("event");
    }

    private ReturnChannelRegistration channelFor(Trigger trigger) {
        return channelByNode.computeIfAbsent(trigger.getHost().getNode(),
                node -> node.getFeature(ReturnChannelMap.class)
                        .registerChannel(this::dispatch));
    }

    private void dispatch(ArrayNode args) {
        // Only registered when the callbacks were given
        switch (args.get(0).asString()) {
        case "click" -> Objects.requireNonNull(onClick).run();
        case "error" -> Objects.requireNonNull(onError).accept(readError(args));
        default -> {
            // Notifications shown by a trigger have no close callback
        }
        }
    }

    /**
     * Reads the error the client reports through a notification's return
     * channel as {@code ('error', reason, message)}. A reason this server does
     * not know, for example from a newer client, is read as
     * {@link Reason#UNKNOWN}.
     *
     * @param args
     *            the arguments the client passed to the channel, not
     *            {@code null}
     * @return the error, not {@code null}
     */
    public static WebNotificationError readError(ArrayNode args) {
        JsonNode reason = args.get(1);
        JsonNode message = args.get(2);
        Reason parsed;
        try {
            parsed = reason == null ? Reason.UNKNOWN
                    : Reason.valueOf(reason.asString());
        } catch (IllegalArgumentException e) {
            parsed = Reason.UNKNOWN;
        }
        return new WebNotificationError(parsed,
                message == null || message.isNull() ? "" : message.asString());
    }
}
