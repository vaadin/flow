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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;

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

    private PushService pushService;

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
     *            Subject used in the JWT payload (for VAPID).
     */
    public WebPush(String publicKey, String privateKey, String subject) {
        this.publicKey = publicKey;

        Security.addProvider(new BouncyCastleProvider());
        try {
            // Initialize push service with the public key, private key and
            // subject
            pushService = PushService.builder().withVapidPublicKey(publicKey)
                    .withVapidPrivateKey(privateKey).withVapidSubject(subject)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new WebPushException(
                    "Security exception initializing web push PushService", e);
        }
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
     */
    public void sendNotification(WebPushSubscription subscription,
            WebPushMessage message) throws WebPushException {
        int statusCode = -1;
        HttpResponse<String> response = null;
        try {
            Subscription.Keys keys = null;
            if (subscription.keys() != null) {
                keys = new Subscription.Keys(subscription.keys().p256dh(),
                        subscription.keys().auth());
            }
            Subscription nativeSubscription = new Subscription(
                    subscription.endpoint(), keys);
            Notification notification = Notification.builder()
                    .subscription(nativeSubscription).payload(message.toJson())
                    .build();
            response = pushService.send(notification,
                    PushService.DEFAULT_ENCODING,
                    HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode();
        } catch (Exception e) {
            getLogger().error("Failed to send notification.", e);
            throw new WebPushException(
                    "Sending of web push notification failed", e);
        }
        if (statusCode != 201) {
            getLogger().error(
                    "Failed to send web push notification, received status code:"
                            + statusCode);
            getLogger().error(String.join("\n", response.body()));
            throw new WebPushException(
                    "Sending of web push notification failed with status code "
                            + statusCode);
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

        executeJavascript(ui,
                "return window.Vaadin.Flow.webPush.registrationStatus()")
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

        executeJavascript(ui,
                "return window.Vaadin.Flow.webPush.notificationDenied()")
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

        executeJavascript(ui,
                "return window.Vaadin.Flow.webPush.notificationGranted()")
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
        executeJavascript(ui, "return window.Vaadin.Flow.webPush.subscribe($0)",
                publicKey).then(resultHandler, errorHandler);
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
        executeJavascript(ui, "return window.Vaadin.Flow.webPush.unsubscribe()")
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
        executeJavascript(ui,
                "return window.Vaadin.Flow.webPush.getSubscription()")
                .then(handlePossiblyEmptySubscription(receiver), errorHandler);
    }

    private PendingJavaScriptResult executeJavascript(UI ui, String script,
            Serializable... parameters) {
        initWebPushClient(ui);
        return ui.getPage().executeJs(script, parameters);
    }

    private void initWebPushClient(UI ui) {
        if (ComponentUtil.getData(ui, "webPushInitialized") != null) {
            return;
        } else {
            ComponentUtil.setData(ui, "webPushInitialized", true);
        }
        Page page = ui.getPage();
        try (InputStream stream = WebPush.class.getClassLoader()
                .getResourceAsStream("META-INF/frontend/FlowWebPush.js")) {
            page.executeJs(StringUtil.removeComments(
                    IOUtils.toString(stream, StandardCharsets.UTF_8)))
                    .then(unused -> getLogger()
                            .debug("Webpush client code initialized"),
                            err -> getLogger().error(
                                    "Webpush client code initialization failed: {}",
                                    err));
        } catch (IOException ioe) {
            throw new WebPushException("Could not load webpush client code");
        }
    }

    private SerializableConsumer<JsonNode> handlePossiblyEmptySubscription(
            WebPushSubscriptionResponse receiver) {
        return json -> {
            ObjectNode responseJson;
            // It may happen that an error is sent as a plain string
            if (json.getNodeType() == JsonNodeType.STRING) {
                responseJson = JacksonUtils.createObjectNode();
                responseJson.put("message", json.asText());
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
                subscriptionJson.get("keys").get("p256dh").asText(),
                subscriptionJson.get("keys").get("auth").asText());
        return new WebPushSubscription(
                subscriptionJson.get("endpoint").asText(), keys);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(WebPush.class);
    }
}
