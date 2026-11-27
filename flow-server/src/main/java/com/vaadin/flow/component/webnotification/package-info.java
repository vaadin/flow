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
/**
 * Server-side access to the browser's Notifications API, which shows native
 * operating system notifications while the application is open in a browser tab
 * — for example to tell the user that a background job finished while they are
 * looking at another tab or application.
 * <p>
 * Use
 * {@link com.vaadin.flow.component.webnotification.WebNotification#onClick(com.vaadin.flow.component.Component)
 * WebNotification.onClick(component)} to ask for permission when a component is
 * clicked,
 * {@link com.vaadin.flow.component.webnotification.WebNotification#permissionSignal()}
 * to observe whether notifications are allowed, and
 * {@link com.vaadin.flow.component.webnotification.WebNotification#show(String)}
 * to show a notification.
 * <p>
 * Permission is requested through {@code onClick} rather than an ordinary
 * server-side click listener because browsers only show the permission prompt
 * while handling a genuine user gesture, which is no longer valid by the time a
 * server round trip completes. Showing a notification once permission is
 * granted needs no gesture and works from any server-side code, including
 * background threads through {@code UI.access}.
 * <p>
 * This is not the same as {@code com.vaadin.flow.component.notification
 * .Notification}, which shows a toast inside the page, nor as Web Push, which
 * delivers messages through a service worker when the application is not open.
 */
@NullMarked
package com.vaadin.flow.component.webnotification;

import org.jspecify.annotations.NullMarked;
