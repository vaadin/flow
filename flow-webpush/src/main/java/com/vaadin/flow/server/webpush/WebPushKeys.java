/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * @since 24.6
 */
public record WebPushKeys(String p256dh, String auth) implements Serializable {
}
