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
package com.vaadin.flow.server.webpush;

import java.io.Serializable;
import java.security.spec.InvalidKeySpecException;

import com.interaso.webpush.VapidKeys;
import com.interaso.webpush.WebPush.SubscriptionState;
import com.interaso.webpush.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * Base class for handling Web Push notifications.
 * <p>
 * Enables developers to register clients to the Push Server, return
 * subscription data to be stored on a server, unregister clients and sending
 * notifications to the clients.
 *
 * @since 24.2
 */
public class WebPush {

    private WebPushService pushService;

    private String publicKey;

    private final SerializableConsumer<String> errorHandler = err -> {
        throw new RuntimeException("Unable to execute web push "
                + "command. JS error is '" + err + "'");
    };

    /**
     * Create new WebPushRegistration for given publicKey.
     *
     * @param publicKey
     *            public key to use for web push
     * @param privateKey
     *            web push private key
     * @param subject
     *            The subject identifying the push notification sender. It must
     *            start with "mailto:" or "https://".
     */
    public WebPush(String publicKey, String privateKey, String subject) {
        this.publicKey = publicKey;

        VapidKeys vapidKeys;

        try {
            // Expect Base64 encoded strings in X509 and PKCS8 formats
            vapidKeys = VapidKeys.create(publicKey, privateKey);
        } catch (Exception e) {
            if (e.getClass().equals(InvalidKeySpecException.class)) {
                // Create from Base64 encoded strings of uncompressed bytes as
                // generated for webpush-java library
                vapidKeys = VapidKeys.fromUncompressedBytes(publicKey,
                        privateKey);
            } else {
                throw e;
            }

        }

        // Initialize push service with the public key, private key and
        // subject
        pushService = new WebPushService(subject, vapidKeys);
    }

    /**
     * Sends Web Push Notification to a client/browser having a given
     * subscription.
     *
     * @param subscription
     *            web push subscription of the client
     * @param message
     *            notification message containing data to be shown, e.g.
     *            <code>title</code> and <code>body</code>
     * @throws WebPushException
     *             if sending a notification fails
     * @since 24.6
     */
    public void sendNotification(WebPushSubscription subscription,
            WebPushMessage message) throws WebPushException {
        SubscriptionState status;
        try {
            status = pushService.send(message.toJson(), subscription.endpoint(),
                    subscription.keys().p256dh(), subscription.keys().auth(),
                    null, null, null);
        } catch (Exception e) {
            getLogger().error("Failed to send notification.", e);
            throw new WebPushException(
                    "Sending of web push notification failed", e);
        }
        if (!SubscriptionState.ACTIVE.equals(status)) {
            getLogger().error(
                    "Failed to send web push notification, received status code: 404 or 410");
            throw new WebPushException(
                    "Sending of web push notification failed with status code 404 or 410");
        }
    }

    /**
     * Check if there is a web push subscription registered to the serviceWorker
     * on the client.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void subscriptionExists(UI ui, WebPushState receiver) {
        final SerializableConsumer<JsonNode> resultHandler = json -> {
            receiver.state(Boolean.parseBoolean(json.toString()));
        };

        ui.getPage().executeJs(WebPushJs.class).registrationStatus()
                .then(resultHandler, errorHandler);
    }

    /**
     * Check if notifications are denied on the client.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void isNotificationDenied(UI ui, WebPushState receiver) {
        final SerializableConsumer<JsonNode> resultHandler = json -> receiver
                .state(Boolean.parseBoolean(json.toString()));

        ui.getPage().executeJs(WebPushJs.class).notificationDenied()
                .then(resultHandler, errorHandler);
    }

    /**
     * Check if notifications are granted on the client.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void isNotificationGranted(UI ui, WebPushState receiver) {
        final SerializableConsumer<JsonNode> resultHandler = json -> receiver
                .state(Boolean.parseBoolean(json.toString()));

        ui.getPage().executeJs(WebPushJs.class).notificationGranted()
                .then(resultHandler, errorHandler);
    }

    /**
     * Subscribe web push for client. Will open an acceptance window for
     * allowing notifications.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void subscribe(UI ui, WebPushSubscriptionResponse receiver) {
        final SerializableConsumer<JsonNode> resultHandler = json -> {
            ObjectNode responseJson = JacksonUtils.readTree(json.toString());
            receiver.subscription(generateSubscription(responseJson));
        };
        ui.getPage().executeJs(WebPushJs.class).subscribe(publicKey)
                .then(resultHandler, errorHandler);
    }

    /**
     * Unsubscribe web push from client.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void unsubscribe(UI ui, WebPushSubscriptionResponse receiver) {
        ui.getPage().executeJs(WebPushJs.class).unsubscribe()
                .then(handlePossiblyEmptySubscription(receiver), errorHandler);
    }

    /**
     * Get an existing subscription from the client.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void fetchExistingSubscription(UI ui,
            WebPushSubscriptionResponse receiver) {
        ui.getPage().executeJs(WebPushJs.class).getSubscription()
                .then(handlePossiblyEmptySubscription(receiver), errorHandler);
    }

    private SerializableConsumer<JsonNode> handlePossiblyEmptySubscription(
            WebPushSubscriptionResponse receiver) {
        return json -> {
            ObjectNode responseJson;
            // It may happen that an error is sent as a plain string
            if (json.getNodeType() == JsonNodeType.STRING) {
                responseJson = JacksonUtils.createObjectNode();
                responseJson.put("message", json.asString());
            } else {
                responseJson = JacksonUtils.readTree(json.toString());
            }
            if (responseJson.has("message")) {
                receiver.subscription(null);
            } else {
                receiver.subscription(generateSubscription(responseJson));
            }
        };
    }

    private WebPushSubscription generateSubscription(
            ObjectNode subscriptionJson) {
        WebPushKeys keys = new WebPushKeys(
                subscriptionJson.get("keys").get("p256dh").asString(),
                subscriptionJson.get("keys").get("auth").asString());
        return new WebPushSubscription(
                subscriptionJson.get("endpoint").asString(), keys);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(WebPush.class);
    }

    /**
     * What web push asks of the browser, as a JavaScript definition for
     * {@link com.vaadin.flow.component.page.Page#executeJs(Class)}.
     */
    @JsDefinition
    public interface WebPushJs extends Serializable {

        /**
         * Checks whether the service worker has a push subscription.
         *
         * @return the pending result, which answers with whether a subscription
         *         exists
         */
        @JsExpression("return window.Vaadin.Flow.webPush.registrationStatus()")
        PendingJavaScriptResult registrationStatus();

        /**
         * Checks whether notifications are denied.
         *
         * @return the pending result, which answers with whether notifications
         *         are denied
         */
        @JsExpression("return window.Vaadin.Flow.webPush.notificationDenied()")
        PendingJavaScriptResult notificationDenied();

        /**
         * Checks whether notifications are granted.
         *
         * @return the pending result, which answers with whether notifications
         *         are granted
         */
        @JsExpression("return window.Vaadin.Flow.webPush.notificationGranted()")
        PendingJavaScriptResult notificationGranted();

        /**
         * Asks for the notification permission and subscribes to web push.
         *
         * @param publicKey
         *            the public key of the application server
         * @return the pending result, which answers with the subscription
         */
        @JsExpression("return window.Vaadin.Flow.webPush.subscribe($0)")
        PendingJavaScriptResult subscribe(String publicKey);

        /**
         * Removes the push subscription of the service worker.
         *
         * @return the pending result, which answers with the removed
         *         subscription, or with a message when there was none
         */
        @JsExpression("return window.Vaadin.Flow.webPush.unsubscribe()")
        PendingJavaScriptResult unsubscribe();

        /**
         * Reads the push subscription of the service worker.
         *
         * @return the pending result, which answers with the subscription, or
         *         with a message when there is none
         */
        @JsExpression("return window.Vaadin.Flow.webPush.getSubscription()")
        PendingJavaScriptResult getSubscription();
    }
}
