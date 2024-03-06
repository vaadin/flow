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
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("beans-no-owner")
public class BeansWithNoOwnerView extends Div {

    @Autowired
    private ApplicationContext context;

    private boolean isSubDiv;

    private Component current;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        NativeButton button = new NativeButton("switch content", ev -> {
            remove(current);
            if (isSubDiv) {
                current = context.getBean(ButtonNoOwner.class);
            } else {
                current = context.getBean(DivNoOwner.class);
            }
            add(current);
            isSubDiv = !isSubDiv;
        });
        button.setId("switch-content");
        add(button);

        RouterLink link = new RouterLink("another-view",
                AnotherBeanNopOwnerView.class);
        link.getElement().getStyle().set("display", "block");
        link.setId("navigate-another");
        add(link);

        current = context.getBean(ButtonNoOwner.class);
        add(current);
    }

}
