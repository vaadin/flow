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

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

@Push
@Route(value = "com.vaadin.flow.uitest.ui.DetatchedTransferProgressListenerView")
public class DetachedTransferProgressListenerView extends Div {

    static final String REMOVED_COMPONENT_DONE = "removed-component-resource-uploaded";
    static final String DOWNLOAD_AND_REMOVE = "download-and-remove-component";

    public DetachedTransferProgressListenerView() {
        Anchor anchor = new Anchor();
        InputStreamDownloadHandler downloadHandler = DownloadHandler
                .fromInputStream(event -> {
                    event.getSession()
                            .accessSynchronously(() -> remove(anchor));
                    return new DownloadResponse(
                            new ByteArrayInputStream(new byte[] { 'a' }),
                            "test.txt", null, 1);
                }).whenComplete(ignore -> {
                    Span completedSpan = new Span("Done");
                    completedSpan.setId(REMOVED_COMPONENT_DONE);
                    add(completedSpan);
                });
        anchor.setText("Download");
        anchor.setHref(downloadHandler);
        anchor.setId(DOWNLOAD_AND_REMOVE);
        add(anchor);
    }
}
