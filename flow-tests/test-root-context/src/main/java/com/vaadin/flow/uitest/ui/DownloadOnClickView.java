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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.download.Download;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Buttons bound with {@link Download#onClick}: one whose handler serves a known
 * body under a file name decided when the file is requested, and one whose
 * handler fails. The IT replaces {@code window.Vaadin.Flow.download.start} with
 * a recording shim and fetches the recorded URLs itself.
 */
@Route(value = "com.vaadin.flow.uitest.ui.DownloadOnClickView", layout = ViewTestLayout.class)
public class DownloadOnClickView extends AbstractDivView {

    static final String BODY = "download-on-click-body";

    static final String FILE_NAME = "lazy-name.txt";

    @Override
    protected void onShow() {
        NativeButton success = new NativeButton("Download");
        success.setId("download-success");
        NativeButton failure = new NativeButton("Download failing");
        failure.setId("download-failure");
        add(success, failure);

        Download.onClick(success).start(
                DownloadHandler.fromInputStream(event -> new DownloadResponse(
                        new ByteArrayInputStream(
                                BODY.getBytes(StandardCharsets.UTF_8)),
                        FILE_NAME, "text/plain", BODY.length())));
        Download.onClick(failure)
                .start(DownloadHandler.fromInputStream(event -> {
                    throw new IllegalStateException("report not available");
                }));
    }
}
