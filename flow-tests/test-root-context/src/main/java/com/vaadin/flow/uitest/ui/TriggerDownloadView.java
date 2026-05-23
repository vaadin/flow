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
package com.vaadin.flow.uitest.ui;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.DownloadAction;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Four buttons exercising each {@link DownloadAction} constructor flavour:
 * static URL, static URL with a filename hint that includes a quote so the JSON
 * escaping round-trips, a {@link DownloadHandler} that streams a known body so
 * the IT can fetch it back, and an {@link Input}-backed {@link PropertyInput}
 * resolved at fire time. The IT replaces
 * {@code window.Vaadin.Flow.download.start} with a recording shim so the
 * assertions don't depend on the browser actually saving the file.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerDownloadView", layout = ViewTestLayout.class)
public class TriggerDownloadView extends AbstractDivView {

    /**
     * Filename suggested by the {@code #download-url-filename} button. The
     * embedded quote is what makes the assertion meaningful — it forces JSON
     * escaping in the rendered JS and proves the value reaches the browser
     * unchanged.
     */
    static final String SUGGESTED_FILENAME = "Q1 \"report\".pdf";

    /**
     * Body served by the {@code #download-handler} button's
     * {@link DownloadHandler}. The IT fetches the recorded URL and asserts this
     * exact string, verifying that {@link Element#setAttribute}-driven
     * stream-resource registration actually wired the URL to the handler.
     */
    static final String HANDLER_BODY = "handler-body-content";

    @Override
    protected void onShow() {
        NativeButton urlButton = new NativeButton("Download URL");
        urlButton.setId("download-url");
        NativeButton urlWithFilenameButton = new NativeButton(
                "Download URL + filename");
        urlWithFilenameButton.setId("download-url-filename");
        NativeButton handlerButton = new NativeButton("Download via handler");
        handlerButton.setId("download-handler");
        NativeButton inputButton = new NativeButton("Download URL from input");
        inputButton.setId("download-input");
        Input urlField = new Input();
        urlField.setId("url-source");

        add(urlButton, urlWithFilenameButton, handlerButton, inputButton,
                urlField);

        new ClickTrigger(urlButton)
                .triggers(new DownloadAction("/static/sample.bin"));
        new ClickTrigger(urlWithFilenameButton).triggers(
                new DownloadAction("/static/sample.bin", SUGGESTED_FILENAME));
        new ClickTrigger(handlerButton)
                .triggers(new DownloadAction((DownloadHandler) event -> {
                    try (OutputStream out = event.getOutputStream()) {
                        out.write(
                                HANDLER_BODY.getBytes(StandardCharsets.UTF_8));
                    }
                }));
        new ClickTrigger(inputButton).triggers(new DownloadAction(
                new PropertyInput<>(urlField, "value", String.class)));
    }
}
