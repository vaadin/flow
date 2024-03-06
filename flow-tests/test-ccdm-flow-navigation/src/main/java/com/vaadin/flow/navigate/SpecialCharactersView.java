/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.navigate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "special åäö $%20'´`")
public class SpecialCharactersView extends Span {

    public SpecialCharactersView() {
        setId("special-view");
        Div div = new Div();
        div.setId("title");
        div.setText("This is the special view");
        add(div);
        RouterLink helloLink = new RouterLink("Hello world view",
                HelloWorldView.class);
        helloLink.setId("navigate-hello");
        add(helloLink);
    }

}
