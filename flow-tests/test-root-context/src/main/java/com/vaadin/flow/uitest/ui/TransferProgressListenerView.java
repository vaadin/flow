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

package com.vaadin.flow.uitest.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DownloadHandler;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Push
@Route(value = "com.vaadin.flow.uitest.ui.TransferProgressListenerView")
public class TransferProgressListenerView extends Div {

    static final String WHEN_START_ID = "for-servlet-resource-when-start";
    static final String ON_PROGRESS_ID = "for-servlet-resource-on-progress";
    static final String ON_ERROR_ID = "for-servlet-resource-on-error";
    static final String ON_COMPLETE_ID = "for-servlet-resource-when-complete";

    public TransferProgressListenerView() {
        Div forServletResourceWhenStart = new Div(
                "File download whenStart status...");
        forServletResourceWhenStart.setId(WHEN_START_ID);
        Div forServletResourceOnProgress = new Div(
                "File download onProgress status...");
        forServletResourceOnProgress.setId(ON_PROGRESS_ID);
        Div forServletResourceOnComplete = new Div(
                "File download whenComplete status...");
        forServletResourceOnComplete.setId(ON_COMPLETE_ID);
        DownloadHandler forFileDownloadHandler = DownloadHandler
                .forServletResource("/images/gift.png").whenStart(() -> {
                    forServletResourceWhenStart
                            .setText("File download whenStart status: started");
                }).onProgress((transfered, total) -> {
                    forServletResourceOnProgress
                            .setText("File download onProgress status: "
                                    + transfered + "/" + total);
                }, 10).whenComplete(success -> {
                    if (success) {
                        forServletResourceOnComplete.setText(
                                "File download whenComplete status: completed");
                    }
                });

        Image image = new Image(forFileDownloadHandler, "no-image");

        add(image);
        add(new Div("Progress:"));
        add(forServletResourceWhenStart, forServletResourceOnProgress,
                forServletResourceOnComplete);

        Div forServletResourceOnError = new Div(
                "File download onError status...");
        forServletResourceOnError.setId(ON_ERROR_ID);
        DownloadHandler errorDownloadHandler = DownloadHandler
                .fromInputStream(req -> {
                    InputStream inputStream = new InputStream() {

                        @Override
                        public int read(byte[] b, int off, int len)
                                throws IOException {
                            throw new IOException("Simulated error");
                        }

                        @Override
                        public int read() throws IOException {
                            return 0;
                        }
                    };
                    return new DownloadResponse(inputStream, "error.txt",
                            "text/plain", -1);
                }).whenComplete(success -> {
                    if (!success) {
                        forServletResourceOnError
                                .setText("File download onError status: error");
                    }
                });

        Image imageError = new Image(errorDownloadHandler, "no-image");

        add(imageError);
        add(new Div("Error:"));
        add(forServletResourceOnError);
    }
}
