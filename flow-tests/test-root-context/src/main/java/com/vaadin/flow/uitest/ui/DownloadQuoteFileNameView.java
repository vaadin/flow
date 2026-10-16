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

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DownloadQuoteFileNameView", layout = ViewTestLayout.class)
public class DownloadQuoteFileNameView extends Div {

    static final String CONTENT = "quoted";

    public DownloadQuoteFileNameView() {
        // The URL postfix puts the file name with the quote into the URL
        DownloadHandler handler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
                event.getWriter().print(CONTENT);
            }

            @Override
            public String getUrlPostfix() {
                return "download-\".json";
            }
        };
        Anchor download = new Anchor(handler,
                "Download with a quote in the file name");
        download.setId("download-quote");
        add(download);
    }
}
