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
