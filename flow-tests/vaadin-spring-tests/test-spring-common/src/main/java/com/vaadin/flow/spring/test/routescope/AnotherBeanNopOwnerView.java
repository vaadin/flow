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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("another-no-owner")
public class AnotherBeanNopOwnerView extends Div {

    public AnotherBeanNopOwnerView(@Autowired DivNoOwner childDiv) {
        setId("another-no-owner");
        add(childDiv);

        RouterLink link = new RouterLink("no-owner-view",
                BeansWithNoOwnerView.class);
        link.getElement().getStyle().set("display", "block");
        link.setId("no-owner-view");
        add(link);
    }
}
