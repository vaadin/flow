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
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DownloadHandlerView", layout = ViewTestLayout.class)
public class DownloadHandlerView extends Div {

    public DownloadHandlerView() {

        DownloadHandler downloadHandler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
                event.getWriter().print("foo");
            }

            @Override
            public String getUrlPostfix() {
                return "file+.jpg";
            }
        };

        Anchor handlerDownload = new Anchor("", "Textual DownloadHandler");
        handlerDownload.setHref(downloadHandler);
        handlerDownload.setId("download-handler-text");

        File jsonFile = new File(getClass().getClassLoader()
                .getResource("download.json").getFile());

        Anchor fileDownload = new Anchor("", "File DownloadHandler shorthand");
        fileDownload.setHref(DownloadHandler.forFile(jsonFile).inline());
        fileDownload.setId("download-handler-file");

        Anchor classDownload = new Anchor("",
                "Class resource DownloadHandler shorthand");
        classDownload.setHref(DownloadHandler
                .forClassResource(this.getClass(), "class-file.json").inline());
        classDownload.setId("download-handler-class");

        Anchor servletDownload = new Anchor("",
                "Servlet resource DownloadHandler shorthand");
        servletDownload.setHref(DownloadHandler
                .forServletResource("/WEB-INF/servlet.json").inline());
        servletDownload.setId("download-handler-servlet");

        DownloadHandler inputStream = DownloadHandler
                .fromInputStream(downloadEvent -> new DownloadResponse(
                        new ByteArrayInputStream(
                                "foo".getBytes(StandardCharsets.UTF_8)),
                        "file+.jpg", "text/plain",
                        "foo".getBytes(StandardCharsets.UTF_8).length))
                .inline();

        Anchor inputStreamDownload = new Anchor("",
                "InputStream DownloadHandler shorthand");
        inputStreamDownload.setHref(inputStream);
        inputStreamDownload.setId("download-handler-input-stream");

        Anchor inputStreamErrorDownload = new Anchor("",
                "InputStream DownloadHandler shorthand (ERROR)");
        inputStreamErrorDownload
                .setHref(DownloadHandler
                        .fromInputStream(downloadEvent -> DownloadResponse
                                .error(HttpStatusCode.INTERNAL_SERVER_ERROR))
                        .inline());
        inputStreamErrorDownload.setId("download-handler-input-stream-error");

        Anchor inputStreamCallbackError = new Anchor("",
                "InputStream DownloadHandler callback shorthand (CALLBACK EXCEPTION)");
        inputStreamCallbackError
                .setHref(DownloadHandler.fromInputStream(downloadEvent -> {
                    throw new IOException("Callback exception");
                }).inline());
        inputStreamCallbackError
                .setId("download-handler-input-stream-callback-error");

        add(handlerDownload, fileDownload, classDownload, servletDownload,
                inputStreamDownload, inputStreamErrorDownload,
                inputStreamCallbackError);

        NativeButton reattach = new NativeButton("Remove and add back",
                event -> {
                    remove(handlerDownload);
                    add(handlerDownload);
                });
        reattach.setId("detach-attach");

        add(reattach);
    }
}
