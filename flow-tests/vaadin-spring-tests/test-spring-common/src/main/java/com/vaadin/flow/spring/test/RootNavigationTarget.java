/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
public class RootNavigationTarget extends Div {

    public RootNavigationTarget(@Autowired DataBean dataBean,
            @Autowired FooNavigationTarget section) {
        setId("main");
        NativeLabel label = new NativeLabel(dataBean.getMessage());
        label.setId("message");
        add(label);

        section.addAttachListener(event -> {
            section.setId("singleton-in-ui");
            section.setText("UI singleton");
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        NativeLabel label = new NativeLabel(
                String.valueOf(getUI().get().getUIId()));
        label.setId("ui-id");
        add(label);

        RouterLink link = new RouterLink("foo", FooNavigationTarget.class);
        link.setId("foo");
        add(link);
    }

}
