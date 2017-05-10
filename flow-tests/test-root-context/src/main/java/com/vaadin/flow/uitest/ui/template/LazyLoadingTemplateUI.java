/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.WebComponents;
import com.vaadin.annotations.WebComponents.PolyfillVersion;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceRegistration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@WebComponents(PolyfillVersion.V1)
public class LazyLoadingTemplateUI extends UI {

    @Tag("lazy-widget")
    public static class LazyWidget extends PolymerTemplate<Message> {
        public LazyWidget() {
            getModel().setText("foo");
        }

        @EventHandler
        private void valueUpdated() {
            Div div = new Div();
            div.setText(getModel().getText());
            div.addClassName("updated");
            getUI().get().add(div);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        Div div = new Div();
        div.setText("Plain Div Component");
        div.setId("initial-div");
        add(div);

        Div a = new Div();
        add(a);
        LazyWidget template = new LazyWidget();
        template.setId("template");
        a.add(template);

        StreamResourceRegistration registration = getSession()
                .getResourceRegistry()
                .registerResource(getHtmlImportResource());
        getPage().addHtmlImport(registration.getResourceUri().toString(),
                false);
    }

    private StreamResource getHtmlImportResource() {
        return new StreamResource("LazyWidget.html", () -> {
            // @formatter:off
            String js = "<link rel='import' href='/v2/bower_components/polymer/polymer.html'>"+
            "<dom-module id='lazy-widget'>"+
                  "<template>"+
                    "<div id='msg' >[[text]]</div>"+
                    "<input id='input' value='{{text::input}}' on-change='valueUpdated'>"+
                  "</template>"+
                  "<script>"+
                   " class LazyWidget extends Polymer.Element {"+
                    "  static get is() { return 'lazy-widget' }"+
                    "}"+
                    "customElements.define(LazyWidget.is, LazyWidget);"+
                  "</script>"+
            "</dom-module>";
            // @formatter:on

            // Wait to ensure that client side will stop until the JavaScript is
            // loaded
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            return new ByteArrayInputStream(
                    js.getBytes(StandardCharsets.UTF_8));
        });
    }
}
