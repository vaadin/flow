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
