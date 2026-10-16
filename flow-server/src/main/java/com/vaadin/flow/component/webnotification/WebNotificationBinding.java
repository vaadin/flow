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

import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.RequestWebNotificationPermissionAction;
import com.vaadin.flow.component.trigger.internal.ShowWebNotificationAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Fluent surface returned from {@link WebNotification#onClick}, used to declare
 * what a click does with notifications. The action runs in the browser at click
 * time, while the user gesture that browsers require for the permission prompt
 * is still valid.
 *
 * <pre>{@code
 * Button enable = new Button("Enable notifications");
 * WebNotification.onClick(enable).requestPermission();
 * }</pre>
 */
public final class WebNotificationBinding implements Serializable {

    private final Trigger trigger;

    WebNotificationBinding(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger);
    }

    /**
     * Asks the user for permission to show notifications when the underlying
     * trigger fires. The browser shows its permission prompt if the user has
     * not decided yet, and does nothing if they already have.
     * <p>
     * The answer arrives through {@link WebNotification#permissionSignal()}, so
     * react to it there — for example by hiding the button once permission is
     * {@link WebNotificationPermission#GRANTED}.
     */
    public void requestPermission() {
        trigger.triggers(new RequestWebNotificationPermissionAction());
    }

    /**
     * Shows a notification when the underlying trigger fires, without a round
     * trip to the server. The user must have granted permission; otherwise the
     * browser shows nothing.
     *
     * @param title
     *            the title of the notification, not {@code null}
     * @param options
     *            what the notification shows besides its title, and how it
     *            behaves, not {@code null}
     */
    public void show(String title, WebNotificationOptions options) {
        Objects.requireNonNull(title, "title must not be null");
        trigger.triggers(new ShowWebNotificationAction(
                new LiteralInput<>(title), options));
    }

    /**
     * Shows a notification when the underlying trigger fires, and reports
     * clicks on it and failures to show it back to the server. Same as
     * {@link #show(String, WebNotificationOptions)} otherwise. The callbacks
     * are shared by every notification shown through this binding.
     *
     * @param title
     *            the title of the notification, not {@code null}
     * @param options
     *            what the notification shows besides its title, and how it
     *            behaves, not {@code null}
     * @param onClick
     *            invoked on the UI thread when the user clicks a notification
     *            shown through this binding, not {@code null}
     * @param onError
     *            invoked on the UI thread when the browser did not show a
     *            notification, for example because permission has not been
     *            granted, not {@code null}
     */
    public void show(String title, WebNotificationOptions options,
            SerializableRunnable onClick,
            SerializableConsumer<WebNotificationError> onError) {
        Objects.requireNonNull(title, "title must not be null");
        trigger.triggers(new ShowWebNotificationAction(
                new LiteralInput<>(title), options, onClick, onError));
    }
}
