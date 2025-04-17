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

package com.vaadin.flow.webpush;

import java.util.List;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.webpush.WebPush;
import com.vaadin.flow.server.webpush.WebPushMessage;
import com.vaadin.flow.server.webpush.WebPushSubscription;

@Route("")
public class WebPushView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String CHECK_ID = "check";
    public static final String SUBSCRIBE_ID = "subscribe";
    public static final String UNSUBSCRIBE_ID = "unsubscribe";
    public static final String NOTIFY_ID = "notify";
    private static final String PUBLIC_KEY = "BPXZkCj3rxN6a1v21aCyMQHmTaAn1QZyWRDeBfwQ4qperQNszSD9JhnZv9b45vHLQLxnK3zsCvCl1r8EDpPDjoM";
    private static final String PRIVATE_KEY = "W-J0f4QwsjrEwDnJJTky5waIX9xNaM87-Dfd42_SEDM";
    public static final String TEST_TITLE = "Test title";

    private int eventCounter = 0;

    NativeButton check, subscribe, unsubscribe, notify;

    WebPush webPush;

    private final Div log;
    private final WebPushAction webPushAction = new WebPushAction(
            "dashboard",
            "Open Dashboard",
            "https://upload.wikimedia.org/wikipedia/commons/0/0e/Message-icon-blue-symbol-double.png"
    );

    private WebPushSubscription subscription;

    public WebPushView() {
        webPush = new WebPush(PUBLIC_KEY, PRIVATE_KEY, "test");

        check = new NativeButton("Check",
                event -> webPush.subscriptionExists(
                        event.getSource().getUI().get(),
                        result -> addLogEntry("Subscription " + result)));
        check.setId(CHECK_ID);

        subscribe = new NativeButton("Subscribe", event -> webPush
                .subscribe(event.getSource().getUI().get(), result -> {
                    addLogEntry("Subscribed " + result.endpoint());
                    subscription = result;
                }));
        subscribe.setId(SUBSCRIBE_ID);

        unsubscribe = new NativeButton("Unsubscribe", event -> webPush
                .unsubscribe(event.getSource().getUI().get(), result -> {
                    String endpoint = result != null ? result.endpoint()
                            : "<unknown>";
                    addLogEntry("Unsubscribed " + endpoint);
                    subscription = null;
                }));
        unsubscribe.setId(UNSUBSCRIBE_ID);

        notify = new NativeButton("Notify", event -> {
            if (subscription != null) {
                WebPushOptions webPushOptions = new WebPushOptions(
                        List.of(webPushAction),
                        "https://upload.wikimedia.org/wikipedia/commons/0/0e/Message-icon-blue-symbol-double.png",
                        "Testing notification",
                        "This is my data!",
                        "rtl",
                        "https://upload.wikimedia.org/wikipedia/commons/0/0e/Message-icon-blue-symbol-double.png",
                        "https://upload.wikimedia.org/wikipedia/commons/0/0e/Message-icon-blue-symbol-double.png",
                        "de-DE",
                        true,
                        true,
                        false,
                        "My Notification",
                        System.currentTimeMillis(),
                        List.of(500, 500, 500)
                );

                webPush.sendNotification(subscription,
                        new WebPushMessage(TEST_TITLE, webPushOptions));
                addLogEntry("Sent notification");
            } else {
                addLogEntry("No notification sent due to missing subscription");
            }
        });
        notify.setId(NOTIFY_ID);

        log = new Div(new Text("Click events and their sources:"));
        log.setId(EVENT_LOG_ID);

        add(check, subscribe, unsubscribe, notify, log);
    }

    private void addLogEntry(String eventDetails) {
        Div div = new Div();
        eventCounter++;
        div.add(eventCounter + ": " + eventDetails);
        div.setId("event-" + eventCounter);
        log.add(div);
    }
}
