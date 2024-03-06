/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.server.StreamResource;

public class StreamResourceExporter extends WebComponentExporter<Div> {

    public StreamResourceExporter() {
        super("vaadin-stream-resource");
    }

    @Override
    protected void configureInstance(WebComponent<Div> webComponent,
            Div component) {
        StreamResource relativeUrl = createStreamResource("relativeURL");
        Anchor relativeLink = new Anchor(relativeUrl, "Relative Link");
        relativeLink.setId("relativeLink");

        StreamResource absoluteUrl = createStreamResource("absoluteURL");
        Anchor absoluteLink = new Anchor(absoluteUrl, "Absolute Link");
        absoluteLink.setId("absoluteLink");

        StreamResource schemalessUrl = createStreamResource(
                "absoluteURLschemaless");
        Anchor schemalessLink = new Anchor(schemalessUrl, "Schemaless Link");
        schemalessLink.setId("schemalessLink");

        component.add(relativeLink, absoluteLink, schemalessLink);
    }

    private StreamResource createStreamResource(String type) {
        return new StreamResource(type + ".txt", (stream, session) -> stream
                .write(type.getBytes(StandardCharsets.UTF_8)));
    }

}
