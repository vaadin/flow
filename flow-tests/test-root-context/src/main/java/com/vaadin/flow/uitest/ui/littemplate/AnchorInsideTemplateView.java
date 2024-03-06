/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Tag("anchor-in-template")
@JsModule("./lit-templates/AnchorInTemplate.js")
@Route("com.vaadin.flow.uitest.ui.littemplate.AnchorInsideTemplateView")
public class AnchorInsideTemplateView extends LitTemplate {

    @Id("anchor")
    private Anchor anchor;

    public AnchorInsideTemplateView() {
        setId("template-with-anchor");
        StreamResource streamResource = new StreamResource("test.txt",
                () -> new ByteArrayInputStream(
                        "this is a test".getBytes(StandardCharsets.UTF_8)));
        anchor.setHref(streamResource);
    }
}
