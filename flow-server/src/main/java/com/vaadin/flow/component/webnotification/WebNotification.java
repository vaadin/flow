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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.signals.Signal;

/**
 * Entry point for the browser's
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API">
 * Notifications API</a>, which shows native operating system notifications
 * while the application is open in a browser tab. Use it to tell the user that
 * something happened while they are looking at another tab or application — a
 * long-running job finished, a message arrived.
 * <ul>
 * <li>{@link #onClick(Component)} — ask for permission when the user clicks a
 * component. Browsers only show the permission prompt during a user gesture, so
 * this is the only reliable way to ask.</li>
 * <li>{@link #permissionSignal()} — whether notifications are allowed, kept up
 * to date when the user changes their mind.</li>
 * <li>{@link #show(String, WebNotificationOptions)} — show a notification and
 * get a {@link WebNotificationHandle} for reacting to clicks.</li>
 * </ul>
 *
 * <pre>{@code
 * Button enable = new Button("Enable notifications");
 * WebNotification.onClick(enable).requestPermission();
 * Signal.effect(enable, () -> enable.setVisible(WebNotification
 *         .permissionSignal().get() == WebNotificationPermission.PROMPT));
 *
 * // Later, for example from a background job through ui.access(...)
 * if (WebNotification.permissionSignal(ui)
 *         .peek() == WebNotificationPermission.GRANTED) {
 *     WebNotification
 *             .show("Report ready",
 *                     WebNotificationOptions.builder()
 *                             .body("The March report finished").build(),
 *                     ui)
 *             .addClickListener(() -> ui.navigate(ReportView.class));
 * } else {
 *     Notification.show("The March report finished");
 * }
 * }</pre>
 *
 * To notify only while the user is away from the tab, check
 * {@code ui.getPage().pageVisibilitySignal()} before showing a notification.
 * <p>
 * <b>Browser caveats.</b>
 * <ul>
 * <li>The Notifications API requires a secure context (HTTPS or
 * {@code localhost}); elsewhere the permission is
 * {@link WebNotificationPermission#UNSUPPORTED}.</li>
 * <li>Chrome on Android only shows notifications through a service worker, so
 * {@link #show(String)} reports a {@link WebNotificationError.Reason#UNKNOWN}
 * error there although the permission can be granted.</li>
 * <li>Notifications are only shown while the page is open. To reach users when
 * the application is closed, use Web Push instead.</li>
 * </ul>
 */
public final class WebNotification {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebNotification.class);

    private static final WebNotificationOptions DEFAULT_OPTIONS = WebNotificationOptions
            .builder().build();

    private WebNotification() {
        // utility class
    }

    /**
     * Starts a notification action bound to clicks on the given component — the
     * entry point for "Enable notifications" buttons. Chain the action onto the
     * returned {@link WebNotificationBinding}:
     *
     * <pre>{@code
     * Button enable = new Button("Enable notifications");
     * WebNotification.onClick(enable).requestPermission();
     * }</pre>
     *
     * The browser only shows the permission prompt while it is handling a
     * genuine user gesture, and that activation is gone by the time a normal
     * server-side click listener runs. Binding the action here makes it run
     * inside the browser's own click handler, where the gesture is still valid
     * — which is why permission is requested through {@code onClick} rather
     * than an ordinary {@code addClickListener}.
     *
     * @param component
     *            the component whose clicks trigger the action, not
     *            {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a binding for chaining the action to perform on click
     */
    public static <T extends Component & ClickNotifier<?>> WebNotificationBinding onClick(
            T component) {
        Objects.requireNonNull(component, "component must not be null");
        return new WebNotificationBinding(new ClickTrigger(component));
    }

    /**
     * Returns a read-only signal telling whether the browser lets the current
     * UI show notifications.
     * <p>
     * The value is seeded during bootstrap and follows the user's decisions
     * afterwards: it changes when the user answers the permission prompt, and
     * when the user changes the setting in the browser's site settings — at the
     * latest when they return to the tab. Reading it never shows a prompt.
     * <p>
     * Use {@code Signal.effect(owner, ...)} to react to changes and
     * {@code .peek()} for a snapshot outside a reactive context.
     *
     * @return the read-only permission signal
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<WebNotificationPermission> permissionSignal() {
        return permissionSignal(UI.getCurrentOrThrow());
    }

    /**
     * Returns a read-only signal telling whether the browser lets the given UI
     * show notifications. Same semantics as {@link #permissionSignal()}; use
     * this overload from background threads or anywhere {@link UI#getCurrent()}
     * is unreliable.
     *
     * @param ui
     *            the UI to read the signal from, not {@code null}
     * @return the read-only permission signal
     */
    public static Signal<WebNotificationPermission> permissionSignal(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        ui.getInternals().ensureWebNotificationPermissionWired();
        return ui.getInternals().getWebNotificationPermissionSignalReadOnly();
    }

    /**
     * Shows a notification with the given title on the current UI. Needs no
     * user gesture, but the user must have granted permission; otherwise the
     * error listeners of the returned handle receive
     * {@link WebNotificationError.Reason#NOT_ALLOWED}.
     *
     * @param title
     *            the title of the notification, not {@code null}
     * @return a handle for reacting to clicks and closing the notification
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static WebNotificationHandle show(String title) {
        return show(title, DEFAULT_OPTIONS, UI.getCurrentOrThrow());
    }

    /**
     * Shows a notification with the given title on the given UI. Same as
     * {@link #show(String)}; use this overload from background threads or
     * anywhere {@link UI#getCurrent()} is unreliable. Like any change to the
     * UI, the call must hold the session lock, so call it inside
     * {@code ui.access(...)} from a background thread.
     *
     * @param title
     *            the title of the notification, not {@code null}
     * @param ui
     *            the UI to show the notification through, not {@code null}
     * @return a handle for reacting to clicks and closing the notification
     */
    public static WebNotificationHandle show(String title, UI ui) {
        return show(title, DEFAULT_OPTIONS, ui);
    }

    /**
     * Shows a notification with the given title on the current UI, with a body
     * text, an icon, a tag or other options. Same as {@link #show(String)}
     * otherwise.
     *
     * @param title
     *            the title of the notification, not {@code null}
     * @param options
     *            what the notification shows besides its title, and how it
     *            behaves, not {@code null}
     * @return a handle for reacting to clicks and closing the notification
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static WebNotificationHandle show(String title,
            WebNotificationOptions options) {
        return show(title, options, UI.getCurrentOrThrow());
    }

    /**
     * Shows a notification with the given title on the given UI, with a body
     * text, an icon, a tag or other options. Same as
     * {@link #show(String, WebNotificationOptions)}; use this overload from
     * background threads or anywhere {@link UI#getCurrent()} is unreliable.
     * Like any change to the UI, the call must hold the session lock, so call
     * it inside {@code ui.access(...)} from a background thread.
     *
     * @param title
     *            the title of the notification, not {@code null}
     * @param options
     *            what the notification shows besides its title, and how it
     *            behaves, not {@code null}
     * @param ui
     *            the UI to show the notification through, not {@code null}
     * @return a handle for reacting to clicks and closing the notification
     */
    public static WebNotificationHandle show(String title,
            WebNotificationOptions options, UI ui) {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(ui, "ui must not be null");
        return new WebNotificationHandle(ui, title, options);
    }

    /**
     * Closes the notifications with the given
     * {@linkplain WebNotificationOptions#tag() tag} that the current UI has
     * shown and that are still visible. Use it to withdraw a notification when
     * whatever it was about has been handled in the application.
     *
     * @param tag
     *            the tag of the notifications to close, not {@code null}
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void closeByTag(String tag) {
        closeByTag(tag, UI.getCurrentOrThrow());
    }

    /**
     * Closes the notifications with the given
     * {@linkplain WebNotificationOptions#tag() tag} that the given UI has shown
     * and that are still visible. Same as {@link #closeByTag(String)}; use this
     * overload from background threads or anywhere {@link UI#getCurrent()} is
     * unreliable. Like any change to the UI, the call must hold the session
     * lock, so call it inside {@code ui.access(...)} from a background thread.
     *
     * @param tag
     *            the tag of the notifications to close, not {@code null}
     * @param ui
     *            the UI that showed the notifications, not {@code null}
     */
    public static void closeByTag(String tag, UI ui) {
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(ui, "ui must not be null");
        ui.getElement().executeJs(WebNotificationJs.class).closeByTag(tag)
                .then(ignored -> {
                }, err -> LOGGER.debug(
                        "Client-side webNotification.closeByTag failed: {}",
                        err));
    }

    /**
     * What web notifications ask of their client-side bridge, as a JavaScript
     * definition for {@link Element#executeJs(Class)}.
     */
    @JsDefinition
    public interface WebNotificationJs extends Serializable {

        /**
         * Shows a notification, reporting clicks, closing and failures through
         * the given channel.
         *
         * @param title
         *            the title of the notification
         * @param options
         *            what the notification shows besides its title
         * @param key
         *            what {@link #close(String)} closes the notification by
         * @param channel
         *            the channel the browser reports what happens to the
         *            notification through
         * @return the pending result, which completes once the notification is
         *         shown or has failed
         */
        @JsExpression("window.Vaadin.Flow.webNotification.show($0, $1, $2, $3)")
        PendingJavaScriptResult show(String title,
                WebNotificationOptions options, String key,
                ReturnChannelRegistration channel);

        /**
         * Closes the notification shown under the given key.
         *
         * @param key
         *            the key the notification was shown with
         * @return the pending result, which completes once the close is
         *         requested
         */
        @JsExpression("window.Vaadin.Flow.webNotification.close($0)")
        PendingJavaScriptResult close(String key);

        /**
         * Closes the notifications shown with the given tag.
         *
         * @param tag
         *            the tag of the notifications to close
         * @return the pending result, which completes once the close is
         *         requested
         */
        @JsExpression("window.Vaadin.Flow.webNotification.closeByTag($0)")
        PendingJavaScriptResult closeByTag(String tag);
    }
}
