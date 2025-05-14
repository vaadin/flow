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
 * Holds the keys that used to encrypt the Web Push notification payload, so
 * that only the current browser can read (decrypt) the content of
 * notifications.
 *
 * @param p256dh
 *            public key on the P-256 curve.
 * @param auth
 *            An authentication secret.
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/API/PushSubscription/getKey">PushSubscription
 *      Keys mdn web docs</a>
 */
public record WebPushKeys(String p256dh, String auth) implements Serializable {
}
