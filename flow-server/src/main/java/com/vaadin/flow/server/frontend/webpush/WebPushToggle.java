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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

@Tag("web-push-toggle")
@JsModule("./WebPushToggle.ts")
public class WebPushToggle extends Component {

    public WebPushToggle(String publicKey) {
        setPublicKey(publicKey);
    }

    public void setPublicKey(String publicKey) {
        getElement().setProperty("publicKey", publicKey);
    }

    public void setCaption(String caption) {
        getElement().setProperty("caption", caption);
    }

    // Events

    public static class WebPushSubscriptionEvent
            extends ComponentEvent<WebPushToggle> {

        String endpoint;
        String auth;
        String p256dh;

        public WebPushSubscriptionEvent(WebPushToggle source,
                boolean fromClient,  String endpoint,
        String auth,
        String p256dh) {
            super(source, fromClient);
            this.endpoint = endpoint;
            this.auth = auth;
            this.p256dh = p256dh;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getAuth() {
            return auth;
        }

        public String getP256dh() {
            return p256dh;
        }
    }

    @DomEvent("web-push-subscribed")
    public static class SubscribeEvent extends WebPushSubscriptionEvent {
        public SubscribeEvent(WebPushToggle source, boolean fromClient,
                @EventData("event.detail.endpoint") String endpoint,
                @EventData("event.detail.keys.auth") String auth,
                @EventData("event.detail.keys.p256dh") String p256dh) {
            super(source, fromClient, endpoint, p256dh, auth);
        }
    }

    @DomEvent("web-push-unsubscribed")
    public static class UnsubscribeEvent extends WebPushSubscriptionEvent {

        public UnsubscribeEvent(WebPushToggle source, boolean fromClient,
                @EventData("event.detail.endpoint") String endpoint,
                @EventData("event.detail.keys.auth") String auth,
                @EventData("event.detail.keys.p256dh") String p256dh) {

            super(source, fromClient, endpoint, p256dh, auth);
        }
    }

    public Registration addSubscribeListener(
            ComponentEventListener<SubscribeEvent> listener) {
        return addListener(SubscribeEvent.class, listener);
    }

    public Registration addUnsubscribeListener(
            ComponentEventListener<UnsubscribeEvent> listener) {
        return addListener(UnsubscribeEvent.class, listener);
    }
}
