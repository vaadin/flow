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

@Route("com.vaadin.flow.uitest.ui.template.InvisibleDefaultPropertyValueView")
public class InvisibleDefaultPropertyValueView extends AbstractDivView {

    public InvisibleDefaultPropertyValueView() {
        PolymerDefaultPropertyValue template = new PolymerDefaultPropertyValue();
        template.setVisible(false);
        template.setId("template");
        add(template);

        Div div = new Div();
        div.setId("email-value");
        add(div);

        add(createButton("Show email value", "show-email",
                event -> div.setText(template.getEmail())));

        add(createButton("Make template visible", "set-visible",
                event -> template.setVisible(true)));
    }

}
