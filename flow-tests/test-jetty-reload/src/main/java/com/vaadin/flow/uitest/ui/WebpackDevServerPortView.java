/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.WebpackDevServerPortView", layout = ViewTestLayout.class)
public class WebpackDevServerPortView extends Div {

    public static final String UUID_ID = "uuid";
    public static final String WEBPACK_PORT_ID = "webpackPortId";
    public static final String TRIGGER_RELOAD_ID = "triggerReload";

    private static final UUID uuid = UUID.randomUUID();

    public WebpackDevServerPortView() {
        // Add a unique number to identify reload
        Span unique = new Span(String.valueOf(uuid));
        unique.setId(UUID_ID);
        add(unique);

        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        Span portSpan = new Span(String.valueOf(handler.getPort()));
        portSpan.setId(WEBPACK_PORT_ID);
        add(portSpan);

        final NativeButton triggerButton = new NativeButton("Trigger reload",
                e -> {
                    try {
                        touch(new File(
                                System.getProperty("jetty.scantrigger")));
                    } catch (IOException ioException) {
                        throw new UncheckedIOException(ioException);
                    }
                });
        triggerButton.setId(TRIGGER_RELOAD_ID);
        add(triggerButton);
    }

    private static void touch(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        file.setLastModified(System.currentTimeMillis());
    }
}
