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

import java.io.ByteArrayInputStream;
import java.util.Optional;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.StreamResource;

@JsModule("./src/Dependency.js")
public class ClientSelectComponent extends Div {
    private Span message = new Span();
    private Select select;

    @Tag("dep-element")
    private static class DepElement extends Component {

    }

    public ClientSelectComponent() {
        select = new Select();
        Backend.getAllClients().forEach(
                client -> select.addItem(client, client.getFirsName()));
        select.addValueChangeListener(this::setValue);

        add(select, message);
        Anchor getPdf = new Anchor();
        getPdf.setText("Download PDF");
        getPdf.setId("link");
        getPdf.setHref(createPdfStreamResource());
        getPdf.getElement().setAttribute("download", true);
        add(getPdf);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        add(new DepElement());
    }

    private StreamResource createPdfStreamResource() {
        StreamResource streamResource = new StreamResource("label.pdf",
                () -> new ByteArrayInputStream(new byte[] { 1 }));
        streamResource.setContentType("application/pdf");
        return streamResource;
    }

    private void setValue(
            AbstractField.ComponentValueChangeEvent<Select, String> event) {
        String messageText = "No selection";
        Optional<Object> item = select.getItem();

        if (item.isPresent()) {
            messageText = "Selected: " + ((Client) item.get()).getFullName();
        }

        message.setText(messageText);
    }

    public void setMessageVisible(boolean visible) {
        message.setVisible(visible);
    }
}
