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
    static final String FILE_NAME = "download-\".json";

    public DownloadQuoteFileNameView() {
        // The quote goes both into the URL, through the URL postfix, and into
        // the Content-Disposition header, through the file name
        DownloadHandler handler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
                event.setFileName(FILE_NAME);
                event.getWriter().print(CONTENT);
            }

            @Override
            public String getUrlPostfix() {
                return FILE_NAME;
            }
        };
        Anchor download = new Anchor(handler,
                "Download with a quote in the file name");
        download.setId("download-quote");
        add(download);
    }
}
