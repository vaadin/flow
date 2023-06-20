/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import nl.martijndwars.webpush.Notification;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Base class for handling Web Push notifications.
 * <p>
 * Enables developers to register clients to the Push Server, return
 * subscription data to be stored on a server, unregister clients and sending
 * notifications to the clients.
 * <p>
 * This class doesn't include any implementation for the intercommunication with
 * a third-party Push Server. Abstract method
 * {@link #sendNotification(Subscription, WebPushMessage)} to be extended by
 * developers to use a concrete implementation/library, which sends a
 * notification to Push Server and later on to a browser.
 *
 * @since 24.2
 */
@JsModule("./FlowWebPush.js")
public class WebPush {

    private PushService pushService;

    private final SerializableConsumer<String> errorHandler = err -> {
        throw new RuntimeException("Unable to retrieve extended "
                + "client details. JS error is '" + err + "'");
    };

    /**
     * Create new WebPushRegistration for given publicKey.
     *
     * @param publicKey
     *            public key to use for web push
     */
    public WebPush(String publicKey, String privateKey, String subject) {
        if (!FeatureFlags.get(VaadinService.getCurrent().getContext())
                .isEnabled(FeatureFlags.WEB_PUSH)) {
            getLogger().error(
                    "WebPush feature is not enabled. Enable feature though dev window or feature file.");
            return;
        }

        Security.addProvider(new BouncyCastleProvider());
        try {
            // Initialize push service with the public key, private key and
            // subject
            pushService = new PushService(publicKey, privateKey, subject);
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
    public void sendNotification(Subscription subscription,
            WebPushMessage message) throws WebPushException {
        try {
            HttpResponse response = pushService
                    .send(new Notification(subscription, message.toJson()));
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 201) {
                getLogger().error("Server error, status code:" + statusCode);
                InputStream content = response.getEntity().getContent();
                List<String> strings = IOUtils.readLines(content, "UTF-8");
                getLogger().error(String.join("\n", strings));
            }
        } catch (GeneralSecurityException | IOException | JoseException
                | ExecutionException | InterruptedException e) {
            getLogger().error("Failed to send notification.", e);
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
        final SerializableConsumer<JsonValue> resultHandler = json -> {
            receiver.state(Boolean.parseBoolean(json.toJson()));
        };

        ui.getPage().executeJs(
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
        final SerializableConsumer<JsonValue> resultHandler = json -> receiver
                .state(Boolean.parseBoolean(json.toJson()));

        ui.getPage().executeJs(
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
        final SerializableConsumer<JsonValue> resultHandler = json -> receiver
                .state(Boolean.parseBoolean(json.toJson()));

        ui.getPage().executeJs(
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
        final SerializableConsumer<JsonValue> resultHandler = json -> {
            JsonObject responseJson = Json.parse(json.toJson());
            receiver.subscription(generateSubscription(responseJson));
        };

        ui.getPage()
                .executeJs("return window.Vaadin.Flow.webPush.subscribe($0)",
                        pushService.getPublicKey())
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
        ui.getPage()
                .executeJs("return window.Vaadin.Flow.webPush.unsubscribe()")
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
        ui.getPage()
                .executeJs(
                        "return window.Vaadin.Flow.webPush.getSubscription()")
                .then(handlePossiblyEmptySubscription(receiver), errorHandler);
    }

    private SerializableConsumer<JsonValue> handlePossiblyEmptySubscription(
            WebPushSubscriptionResponse receiver) {
        return json -> {
            JsonObject responseJson = Json.parse(json.toJson());
            if (responseJson.hasKey("message")) {
                receiver.subscription(null);
            } else {
                receiver.subscription(generateSubscription(responseJson));
            }
        };
    }

    private Subscription generateSubscription(JsonObject subscriptionJson) {
        Subscription.Keys keys = new Subscription.Keys(
                subscriptionJson.getObject("keys").getString("p256dh"),
                subscriptionJson.getObject("keys").getString("auth"));
        return new Subscription(subscriptionJson.getString("endpoint"), keys);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(WebPush.class);
    }
}
