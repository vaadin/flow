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

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class UpgradeElementUI extends UI {

    @Tag("upgrade-element")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/UpgradeElement.html")
    public static class UpgradeElement extends PolymerTemplate<Message> {

        @EventHandler
        private void valueUpdated() {
            Label label = new Label(getModel().getText());
            label.setId("text-update");
            getUI().get().add(label);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        UpgradeElement template = new UpgradeElement();
        template.setId("template");

        NativeButton button = new NativeButton("Upgrade element",
                event -> getPage().executeJavaScript(
                        "customElements.define(MyTemplate.is, MyTemplate);"));
        button.setId("upgrade");
        add(template, button);
    }
}
