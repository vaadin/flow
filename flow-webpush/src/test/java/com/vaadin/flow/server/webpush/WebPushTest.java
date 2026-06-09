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

import java.lang.reflect.Field;

import com.interaso.webpush.WebPush.SubscriptionState;
import com.interaso.webpush.WebPushService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebPushTest {

    private static final String PUBLIC_KEY = "BPXZkCj3rxN6a1v21aCyMQHmTaAn1QZyWRDeBfwQ4qperQNszSD9JhnZv9b45vHLQLxnK3zsCvCl1r8EDpPDjoM";
    private static final String PRIVATE_KEY = "W-J0f4QwsjrEwDnJJTky5waIX9xNaM87-Dfd42_SEDM";

    private WebPush webPush;
    private WebPushService pushServiceMock;

    @Before
    public void setUp() throws Exception {
        webPush = new WebPush(PUBLIC_KEY, PRIVATE_KEY,
                "mailto:test@example.com");
        pushServiceMock = mock(WebPushService.class);
        Field field = WebPush.class.getDeclaredField("pushService");
        field.setAccessible(true);
        field.set(webPush, pushServiceMock);
    }

    @Test
    public void sendNotification_subscriptionExpired_throwsWebPushException() {
        when(pushServiceMock.send(any(String.class), any(String.class),
                any(String.class), any(String.class), any(), any(), any()))
                .thenReturn(SubscriptionState.EXPIRED);

        WebPushSubscription subscription = new WebPushSubscription(
                "https://push.example/endpoint",
                new WebPushKeys("p256dhKey", "authKey"));
        WebPushMessage message = new WebPushMessage("title", "body");

        WebPushException ex = assertThrows(WebPushException.class,
                () -> webPush.sendNotification(subscription, message));
        assertEquals(
                "Sending of web push notification failed with status code 404 or 410",
                ex.getMessage());
    }

    @Test
    public void sendNotification_subscriptionActive_doesNotThrow() {
        when(pushServiceMock.send(any(String.class), any(String.class),
                any(String.class), any(String.class), any(), any(), any()))
                .thenReturn(SubscriptionState.ACTIVE);

        WebPushSubscription subscription = new WebPushSubscription(
                "https://push.example/endpoint",
                new WebPushKeys("p256dhKey", "authKey"));
        WebPushMessage message = new WebPushMessage("title", "body");

        webPush.sendNotification(subscription, message);
    }
}
