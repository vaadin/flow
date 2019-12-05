/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import org.jsoup.Jsoup;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.LazyLoadingTemplateView")
public class LazyLoadingTemplateView extends AbstractDivView {

    @Tag("lazy-widget")
    public static class LazyWidget extends PolymerTemplate<Message> {
        public LazyWidget() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse(getTemplateContent())));
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

    public LazyLoadingTemplateView() {
        Div div = new Div();
        div.setText("Plain Div Component");
        div.setId("initial-div");
        add(div);

        Div a = new Div();
        add(a);
        LazyWidget template = new LazyWidget();
        template.setId("template");
        a.add(template);

        StreamRegistration registration = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(getHtmlImportResource());
        getPage().addHtmlImport(
                "base://" + registration.getResourceUri().toString(),
                LoadMode.LAZY);
    }

    private StreamResource getHtmlImportResource() {
        return new StreamResource("LazyWidget.html", () -> {

            // Wait to ensure that client side will stop until the JavaScript is
            // loaded
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            return new ByteArrayInputStream(
                    getTemplateContent().getBytes(StandardCharsets.UTF_8));
        });
    }

    private static String getTemplateContent() {
        // @formatter:off
        return "<link rel='import' href='/frontend/bower_components/polymer/polymer-element.html'>"+
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
    }
}
