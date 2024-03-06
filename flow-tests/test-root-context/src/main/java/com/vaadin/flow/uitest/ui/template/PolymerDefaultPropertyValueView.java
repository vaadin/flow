/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.PolymerDefaultPropertyValueView")
public class PolymerDefaultPropertyValueView extends AbstractDivView {

    public PolymerDefaultPropertyValueView() {
        PolymerDefaultPropertyValue template = new PolymerDefaultPropertyValue();
        template.setId("template");
        add(template);

        add(createButton("Show email value", "show-email",
                event -> createEmailValue(template)));
    }

    private void createEmailValue(PolymerDefaultPropertyValue template) {
        Div div = new Div();
        div.setText(template.getEmail());
        div.setId("email-value");
        add(div);
    }

}
