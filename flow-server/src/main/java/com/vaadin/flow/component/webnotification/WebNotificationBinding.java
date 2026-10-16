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

import com.vaadin.flow.component.trigger.internal.RequestWebNotificationPermissionAction;
import com.vaadin.flow.component.trigger.internal.Trigger;

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
}
