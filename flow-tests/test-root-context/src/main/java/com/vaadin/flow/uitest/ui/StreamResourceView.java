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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.ElementRequestHandler;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.StreamResourceView", layout = ViewTestLayout.class)
public class StreamResourceView extends Div {

    public StreamResourceView() {
        Anchor esrAnchor = new Anchor();
        esrAnchor.setText("esr anchor");
        esrAnchor.setId("esrAnchor");
        StreamResourceRegistry.ElementStreamResource elementStreamResource = new StreamResourceRegistry.ElementStreamResource(
                new ElementRequestHandler() {
                    @Override
                    public void handleRequest(VaadinRequest request,
                            VaadinResponse response, VaadinSession session,
                            Element owner) {
                        response.setContentType("text/plain");
                        try {
                            response.getOutputStream().write(
                                    "foo".getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public String getUrlPostfix() {
                        return "esr-filename.txt";
                    }
                }, esrAnchor.getElement());
        esrAnchor.setHref(elementStreamResource);

        StreamResource resource = new StreamResource("file name",
                () -> new ByteArrayInputStream(
                        "foo".getBytes(StandardCharsets.UTF_8)));
        Anchor download = new Anchor("", "Download filename");
        download.setHref(resource);
        download.setId("link");

        StreamResource plusResource = new StreamResource("file+.jpg",
                () -> new ByteArrayInputStream(
                        "foo".getBytes(StandardCharsets.UTF_8)));
        Anchor plusDownload = new Anchor("", "Download file+.jpg");
        plusDownload.setHref(plusResource);
        plusDownload.setId("plus-link");

        StreamResource percentResource = new StreamResource("file%.jpg",
                () -> new ByteArrayInputStream(
                        "foo".getBytes(StandardCharsets.UTF_8)));
        Anchor percentDownload = new Anchor("", "Download file%.jpg");
        percentDownload.setHref(percentResource);
        percentDownload.setId("percent-link");

        add(esrAnchor, download, plusDownload, percentDownload);

        NativeButton reattach = new NativeButton("Remove and add back",
                event -> {
                    remove(download);
                    add(download);
                });
        reattach.setId("detach-attach");

        add(reattach);
    }
}
