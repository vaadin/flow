/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route("stream-resource")
public class StreamResourceView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        StreamResource resource = new StreamResource("filename",
                () -> new ByteArrayInputStream(
                        "Hello world".getBytes(StandardCharsets.UTF_8)));
        Anchor download = new Anchor("", "Download file");
        download.setHref(resource);
        download.setId("download");
        add(download);
    }
}
