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
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DisabledImageDownloadHandlerView", layout = ViewTestLayout.class)
public class DisabledImageDownloadHandlerView extends Div {

    static final String IMAGE_PAYLOAD = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16">\
            <circle cx="8" cy="8" r="7" fill="#1676f3"/></svg>""";
    static final String IFRAME_PAYLOAD = """
            <!DOCTYPE html><html><body><p>Hello from iframe</p></body></html>""";

    public DisabledImageDownloadHandlerView() {
        Image image = new Image(DownloadHandler.fromInputStream(event -> {
            byte[] bytes = IMAGE_PAYLOAD.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    "icon.svg", "image/svg+xml", bytes.length);
        }), "icon");
        image.setId("disabled-image");
        image.setWidth("100px");
        image.setHeight("100px");

        IFrame iframe = new IFrame(DownloadHandler.fromInputStream(event -> {
            byte[] bytes = IFRAME_PAYLOAD.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    "frame.html", "text/html", bytes.length);
        }));
        iframe.setId("disabled-iframe");
        iframe.setWidth("200px");
        iframe.setHeight("100px");

        Div disabledContainer = new Div(image, iframe);
        disabledContainer.setId("disabled-container");
        disabledContainer.setEnabled(false);

        add(disabledContainer);
    }
}
