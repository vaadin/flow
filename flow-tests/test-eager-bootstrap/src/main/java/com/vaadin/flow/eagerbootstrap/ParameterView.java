/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.eagerbootstrap;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("parameter")
public class ParameterView extends Div implements HasUrlParameter<String> {

    private Div parameters;

    public ParameterView() {
        setId("view");

        RouterLink fooLink = new RouterLink("Navigate with parameter 'foo'",
                ParameterView.class, "foo");
        fooLink.setId("fooLink");
        add(fooLink);
        add(new Div());
        RouterLink barLink = new RouterLink("Navigate with parameter 'bar'",
                ParameterView.class, "bar");
        barLink.setId("barLink");
        add(barLink);

        parameters = new Div();
        parameters.setId("parameters");
        parameters.getStyle()
                .setWhiteSpace(com.vaadin.flow.dom.Style.WhiteSpace.PRE);
        add(parameters);

    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        parameters.setText(parameters.getText() + "\n"
                + "setParameter called with: " + parameter);
    }
}
