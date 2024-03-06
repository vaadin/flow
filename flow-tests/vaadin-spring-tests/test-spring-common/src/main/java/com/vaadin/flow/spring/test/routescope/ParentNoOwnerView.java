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
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

@Route("parent-no-owner")
public class ParentNoOwnerView extends Div implements RouterLayout {

    private @Autowired BeanNoOwner bean;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            RouterLink link = new RouterLink("child", ChildNoOwnerView.class);
            link.setId("to-child");
            add(link);

            Div div = new Div();
            div.setId("parent-info");
            div.getElement().getStyle().set("display", "block");
            div.setText(bean.getValue());
            add(div);
        }
    }
}
