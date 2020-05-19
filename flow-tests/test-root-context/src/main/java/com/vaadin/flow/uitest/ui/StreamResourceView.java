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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.StreamResourceView", layout = ViewTestLayout.class)
public class StreamResourceView extends Div {

    public StreamResourceView() {
        StreamResource resource = new StreamResource("filename",
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

        add(download, plusDownload);

        NativeButton reattach = new NativeButton("Remove and add back",
                event -> {
                    remove(download);
                    add(download);
                });
        reattach.setId("detach-attach");

        add(reattach);
    }
}
