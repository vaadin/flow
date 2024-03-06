/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.UpgradeElementView")
public class UpgradeElementView extends AbstractDivView {

    @Tag("upgrade-element")
    @JsModule("./UpgradeElement.js")
    public static class UpgradeElement extends PolymerTemplate<Message> {

        @EventHandler
        private void valueUpdated() {
            Label label = new Label(getModel().getText());
            label.setId("text-update");
            getUI().get().add(label);
        }
    }

    public UpgradeElementView() {
        UpgradeElement template = new UpgradeElement();
        template.setId("template");

        NativeButton button = createButton("Upgrade element", "upgrade",
                event -> getPage()
                        .executeJs("if ( !window.upgradeElementDefined ) "
                                + "{customElements.define(window.MyTemplate.is, window.MyTemplate);}"));
        add(template, button);
    }
}
