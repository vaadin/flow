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
package com.vaadin.flow.test.cspstrict.springsecurity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * Adds a link to a small image served by a {@link DownloadHandler} to every UI,
 * as an example of a response that Vaadin writes and that is not a bootstrap
 * page.
 * <p>
 * The link is added when the UI is created and not by a view, so that its URL
 * is in the response to the request that creates the UI. A test can then read
 * the URL without a browser: a view is only created by a later navigation
 * request from the client.
 */
@Component
public class DownloadLink implements VaadinServiceInitListener {

    public static final String CONTENT_TYPE = "image/svg+xml";

    private static final byte[] IMAGE = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>"
            .getBytes(StandardCharsets.UTF_8);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInit -> uiInit.getUI()
                .add(new Anchor(DownloadHandler
                        .fromInputStream(download -> new DownloadResponse(
                                new ByteArrayInputStream(IMAGE), "image.svg",
                                CONTENT_TYPE, IMAGE.length)),
                        "Download")));
    }
}
