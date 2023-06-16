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

package com.vaadin.flow.server.frontend.webpush;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableConsumer;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class for handling web push registration to the client and returning
 * subscription data on the server for pushing notifications.
 *
 * @since 24.2
 */
@JsModule("./WebPushRegistration.js")
public abstract class WebPushRegistration {

    private final String publicKey;

    private final SerializableConsumer<String> errorHandler = err -> {
        throw new RuntimeException("Unable to retrieve extended "
                + "client details. JS error is '" + err + "'");
    };

    /**
     * Web push subscription class containing web push registration data.
     */
    public static class WebPushSubscription {
        String endpoint;
        String auth;
        String p256dh;

        /**
         * Subscription constructor.
         *
         * @param endpoint
         *            subscription endpoint
         * @param auth
         *            subscription authority
         * @param p256dh
         *            subscription p256h key
         */
        public WebPushSubscription(String endpoint, String auth,
                String p256dh) {
            this.endpoint = endpoint;
            this.auth = auth;
            this.p256dh = p256dh;
        }

        /**
         * Get the subscription endpoint
         *
         * @return endpoint
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * Get subscription authority
         *
         * @return authority
         */
        public String getAuth() {
            return auth;
        }

        /**
         * Get subscription key
         *
         * @return p256h key
         */
        public String getP256dh() {
            return p256dh;
        }
    }

    /**
     * Callback for receiving web push client-side state boolean.
     */
    @FunctionalInterface
    public interface WebPushState extends Serializable {

        /**
         * Invoked when the client-side details are available.
         *
         * @param state
         *            boolean for requested state
         */
        void state(boolean state);
    }

    /**
     * Callback for receiving web push subscription details
     */
    @FunctionalInterface
    public interface WebPushSubscriptionResponse extends Serializable {

        /**
         * Invoked when the client-side details are available.
         *
         * @param subscription
         *            web push subscription object
         */
        void subscription(WebPushSubscription subscription);
    }

    /**
     * Create new WebPushRegistration for given publicKey.
     *
     * @param publicKey
     *            public key to use for web push
     */
    public WebPushRegistration(String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Get a public key used for registering/sign up on a Push Server.
     *
     * @return public key
     */
    protected String getPublicKey() {
        return publicKey;
    }

    /**
     * Sends Web Push Notification to a client/browser having a given
     * subscription.
     *
     * @param subscription
     *            web push subscription of the client
     * @param messageJson
     *            notification message containing <code>title</code> and
     *            <code>body</code> strings
     * @throws WebPushException
     *             if sending a notification fails
     */
    public abstract void sendNotification(
            WebPushRegistration.WebPushSubscription subscription,
            String messageJson) throws WebPushException;

    /**
     * Check is web push is currently registered on the client.
     *
     * @param ui
     *            current ui
     * @param receiver
     *            the callback to which the details are provided
     */
    public void isWebPushRegistered(UI ui, WebPushState receiver) {
        final SerializableConsumer<JsonValue> resultHandler = json -> {
            receiver.state(Boolean.parseBoolean(json.toJson()));
        };

        ui.getPage().executeJs(
                "return window.Vaadin.Flow.webPush.registrationStatus()")
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
    public void subscribeWebPush(UI ui, WebPushSubscriptionResponse receiver) {
        final SerializableConsumer<JsonValue> resultHandler = json -> {
            JsonObject parse = Json.parse(json.toJson());
            receiver.subscription(
                    new WebPushSubscription(parse.getString("endpoint"),
                            parse.getObject("keys").getString("auth"),
                            parse.getObject("keys").getString("p256dh")));
        };

        ui.getPage()
                .executeJs("return window.Vaadin.Flow.webPush.subscribe($0)",
                        publicKey)
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
    public void unsubscribeWebPush(UI ui,
            WebPushSubscriptionResponse receiver) {
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
    public void getExistingSubscription(UI ui,
            WebPushSubscriptionResponse receiver) {
        ui.getPage()
                .executeJs(
                        "return window.Vaadin.Flow.webPush.getSubscription()")
                .then(handlePossiblyEmptySubscription(receiver), errorHandler);
    }

    private SerializableConsumer<JsonValue> handlePossiblyEmptySubscription(
            WebPushSubscriptionResponse receiver) {
        return json -> {
            JsonObject parse = Json.parse(json.toJson());
            if (parse.hasKey("message")) {
                receiver.subscription(null);
            } else {
                receiver.subscription(
                        new WebPushSubscription(parse.getString("endpoint"),
                                parse.getObject("keys").getString("auth"),
                                parse.getObject("keys").getString("p256dh")));
            }
        };
    }
}
