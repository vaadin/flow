/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("anchor-in-template")
@JsModule("AnchorInTemplate.js")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/AnchorInTemplate.html")
@Route("com.vaadin.flow.uitest.ui.template.AnchorInsideTemplateView")
public class AnchorInsideTemplateView extends PolymerTemplate<TemplateModel> {

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
