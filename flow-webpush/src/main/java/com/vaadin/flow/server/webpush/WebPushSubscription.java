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
package com.vaadin.flow.server.webpush;

import java.io.Serializable;

/**
 * Represents a Web Push subscription that Push Manager gives back, when a user
 * subscribes to push notifications in browser.
 *
 * @param endpoint
 *            a custom URL pointing to a push server, which can be used to send
 *            a push message to the particular service worker instance that
 *            subscribed to the push service. For this reason, it is a good idea
 *            to keep your endpoint a secret, so others do not hijack it and
 *            abuse the push functionality.
 * @param keys
 *            an object containing the keys that used to encrypt the payload.
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/API/PushSubscription">PushSubscription
 *      mdn web docs</a>
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/API/PushManager">PushManager
 *      mdn web docs</a>
 */
public record WebPushSubscription(String endpoint,
        WebPushKeys keys) implements Serializable {
}
