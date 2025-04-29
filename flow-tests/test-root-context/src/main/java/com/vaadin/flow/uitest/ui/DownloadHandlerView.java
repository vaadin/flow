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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.DownloadHandler;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DownloadHandlerView", layout = ViewTestLayout.class)
public class DownloadHandlerView extends Div {

    List<StreamRegistration> registrations = new ArrayList<>();

    public DownloadHandlerView() {

        DownloadHandler downloadHandler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadRequest event) {
                event.getWriter().print("foo");
            }

            @Override
            public String getUrlPostfix() {
                return "file+.jpg";
            }
        };

        StreamRegistration streamRegistration = VaadinSession.getCurrent()
                .getResourceRegistry().registerResource(downloadHandler);
        registrations.add(streamRegistration);

        Anchor handlerDownload = new Anchor("", "Textual DownloadHandler");
        handlerDownload.setHref(streamRegistration.getResource());
        handlerDownload.setId("download-handler-text");

        File jsonFile = new File(getClass().getClassLoader()
                .getResource("download.json").getFile());
        streamRegistration = VaadinSession.getCurrent().getResourceRegistry()
                .registerResource(DownloadHandler.forFile(jsonFile));
        registrations.add(streamRegistration);

        Anchor fileDownload = new Anchor("", "File DownloadHandler shorthand");
        fileDownload.setHref(streamRegistration.getResource());
        fileDownload.setId("download-handler-file");

        streamRegistration = VaadinSession.getCurrent().getResourceRegistry()
                .registerResource(DownloadHandler
                        .forClassResource(this.getClass(), "class-file.json"));
        registrations.add(streamRegistration);

        Anchor classDownload = new Anchor("",
                "Class resource DownloadHandler shorthand");
        classDownload.setHref(streamRegistration.getResource());
        classDownload.setId("download-handler-class");

        streamRegistration = VaadinSession.getCurrent().getResourceRegistry()
                .registerResource(DownloadHandler
                        .forServletResource("/WEB-INF/servlet.json"));
        registrations.add(streamRegistration);

        Anchor servletDownload = new Anchor("",
                "Servlet resource DownloadHandler shorthand");
        servletDownload.setHref(streamRegistration.getResource());
        servletDownload.setId("download-handler-servlet");

        DownloadHandler inputStream = DownloadHandler
                .fromInputStream(downloadEvent -> new DownloadResponse(
                        new ByteArrayInputStream(
                                "foo".getBytes(StandardCharsets.UTF_8)),
                        "file+.jpg", "text/plain",
                        "foo".getBytes(StandardCharsets.UTF_8).length));
        streamRegistration = VaadinSession.getCurrent().getResourceRegistry()
                .registerResource(inputStream);
        registrations.add(streamRegistration);

        Anchor inputStreamDownload = new Anchor("",
                "InputStream DownloadHandler shorthand");
        inputStreamDownload.setHref(streamRegistration.getResource());
        inputStreamDownload.setId("download-handler-input-stream");

        streamRegistration = VaadinSession.getCurrent().getResourceRegistry()
                .registerResource(DownloadHandler
                        .fromInputStream(downloadEvent -> DownloadResponse
                                .error(HttpStatusCode.INTERNAL_SERVER_ERROR)));
        registrations.add(streamRegistration);

        Anchor inputStreamErrorDownload = new Anchor("",
                "InputStream DownloadHandler shorthand");
        inputStreamErrorDownload.setHref(streamRegistration.getResource());
        inputStreamErrorDownload.setId("download-handler-input-stream-error");

        add(handlerDownload, fileDownload, classDownload, servletDownload,
                inputStreamDownload, inputStreamErrorDownload);

        NativeButton reattach = new NativeButton("Remove and add back",
                event -> {
                    remove(handlerDownload);
                    add(handlerDownload);
                });
        reattach.setId("detach-attach");

        add(reattach);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        registrations.forEach(StreamRegistration::unregister);
    }
}
