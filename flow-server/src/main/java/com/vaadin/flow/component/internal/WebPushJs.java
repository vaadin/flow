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
package com.vaadin.flow.component.internal;

import java.io.Serializable;

import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * What web push asks of the browser, as a JavaScript definition for
 * {@link Page#executeJs(Class)}. The functions it calls are installed by the
 * web push helper that the client bundle carries.
 * <p>
 * It lives here rather than next to the web push support, because the bundle is
 * checked against the definitions found on the class path: a definition in an
 * optional module would make a pre-built bundle mismatch every application that
 * does not have that module.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.4
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
     * @return the pending result, which answers with whether notifications are
     *         denied
     */
    @JsExpression("return window.Vaadin.Flow.webPush.notificationDenied()")
    PendingJavaScriptResult notificationDenied();

    /**
     * Checks whether notifications are granted.
     *
     * @return the pending result, which answers with whether notifications are
     *         granted
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
     * @return the pending result, which answers with the removed subscription,
     *         or with a message when there was none
     */
    @JsExpression("return window.Vaadin.Flow.webPush.unsubscribe()")
    PendingJavaScriptResult unsubscribe();

    /**
     * Reads the push subscription of the service worker.
     *
     * @return the pending result, which answers with the subscription, or with
     *         a message when there is none
     */
    @JsExpression("return window.Vaadin.Flow.webPush.getSubscription()")
    PendingJavaScriptResult getSubscription();
}
