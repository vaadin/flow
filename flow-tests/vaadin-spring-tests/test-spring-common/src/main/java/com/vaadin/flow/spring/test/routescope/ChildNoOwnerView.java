/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "child-no-owner", layout = ParentNoOwnerView.class)
public class ChildNoOwnerView extends Div {

    private @Autowired BeanNoOwner bean;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            RouterLink link = new RouterLink("parent", ParentNoOwnerView.class);
            link.setId("to-parent");
            add(link);

            Div div = new Div();
            div.setId("child-info");
            div.getElement().getStyle().set("display", "block");
            div.setText(bean.getValue());
            add(div);
        }
    }
}
