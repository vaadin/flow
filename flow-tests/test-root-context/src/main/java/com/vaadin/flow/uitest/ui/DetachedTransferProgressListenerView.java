/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
@Route(value = "com.vaadin.flow.uitest.ui.DetachedTransferProgressListenerView")
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
