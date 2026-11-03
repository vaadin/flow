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
package com.vaadin.flow.quarkus.it.sseaddon;

import jakarta.servlet.AsyncContext;

import java.lang.reflect.Method;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.ssepushaddon.SsePushConnection;

@Route("")
public class PushView extends Div {

    /**
     * Multibyte characters of two, three and four bytes in UTF-8, and the
     * message delimiter of Atmosphere.
     */
    static final String MESSAGE_PART = "äö€日本😀|";

    private final Span serverCounter = new Span("0");
    private final Span clientCounter = new Span("0");
    private final Div message = new Div();
    private Thread pusher;

    public PushView() {
        serverCounter.setId("serverCounter");
        clientCounter.setId("clientCounter");
        message.setId("message");
        add(new Span("Server counter: "), serverCounter,
                new Span(" Client counter: "), clientCounter,
                new NativeButton("Increment client counter",
                        event -> increment(clientCounter)) {
                    {
                        setId("incrementClientCounter");
                    }
                },
                new NativeButton("Push large message",
                        event -> pushLargeMessage(
                                event.getSource().getUI().orElseThrow())) {
                    {
                        setId("pushLargeMessage");
                    }
                },
                new NativeButton("Lose the stream silently",
                        event -> loseStreamSilently(
                                event.getSource().getUI().orElseThrow())) {
                    {
                        setId("loseStreamSilently");
                    }
                }, message);
    }

    /**
     * Creates a message far larger than the buffers on the way, of
     * {@code repeat} copies of {@link #MESSAGE_PART}.
     *
     * @param repeat
     *            the number of copies
     * @return the message
     */
    static String createLargeMessage(int repeat) {
        return MESSAGE_PART.repeat(repeat);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        add(new Span(ui.getPushConfiguration().getTransportName()) {
            {
                setId("transport");
            }
        });
        pusher = Thread.ofVirtual().start(() -> {
            try {
                while (true) {
                    Thread.sleep(500);
                    ui.access(() -> increment(serverCounter));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        pusher.interrupt();
    }

    private void pushLargeMessage(UI ui) {
        Thread.ofVirtual().start(() -> ui
                .access(() -> message.setText(createLargeMessage(20_000))));
    }

    /**
     * Makes the server give up the stream without ending it, as when writing to
     * it has failed while the client has not noticed, e.g. after a network
     * change. The server holds the pushes for the next stream, and the client
     * sees nothing more on this one.
     */
    private static void loseStreamSilently(UI ui) {
        SsePushConnection connection = (SsePushConnection) ui.getInternals()
                .getPushConnection();
        try {
            Method getAsyncContext = SsePushConnection.class
                    .getDeclaredMethod("getAsyncContext");
            getAsyncContext.setAccessible(true);
            connection.connectionLost(
                    (AsyncContext) getAsyncContext.invoke(connection));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void increment(Span counter) {
        counter.setText(
                String.valueOf(Integer.parseInt(counter.getText()) + 1));
    }
}
