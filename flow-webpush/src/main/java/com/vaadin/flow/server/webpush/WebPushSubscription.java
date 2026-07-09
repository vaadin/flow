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
 * @since 24.6
 */
public record WebPushSubscription(String endpoint,
        WebPushKeys keys) implements Serializable {
}
