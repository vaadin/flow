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

/**
 * Web push subscription class containing web push registration data.
 */
public class WebPushSubscription {
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
    public WebPushSubscription(String endpoint, String auth, String p256dh) {
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