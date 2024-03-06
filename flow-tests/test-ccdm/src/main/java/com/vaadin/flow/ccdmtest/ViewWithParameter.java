/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

@Route(value = "paramview", layout = MainLayout.class)
public class ViewWithParameter extends Div implements HasUrlParameter<String> {

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        String textContent = "Parameter: " + parameter;
        String queryString = event.getLocation().getQueryParameters()
                .getQueryString();
        if (queryString.length() > 0) {
            textContent += " - Query string: " + queryString;
        }
        setText(textContent);
    }
}
