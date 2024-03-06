/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@Route(value = "button-in-layout", layout = Layout.class)
public class ButtonInLayout extends Div {

    private final ApplicationContext context;

    private final String componentId = UUID.randomUUID().toString();

    private final Div buttonScopedBeanCount;

    public ButtonInLayout(
            @Autowired @RouteScopeOwner(Layout.class) MyService service,
            @Autowired ApplicationContext context) {
        this.context = context;
        // the text should be preserved inside route scope
        NativeButton button = new NativeButton(service.getValue());
        button.setId("route-service");
        add(button);

        // the component id should be changed every time when component is
        // created
        Div div = new Div();
        div.setId("component-id");
        div.setText(componentId);
        add(div);

        div = new Div();
        div.setId("no-owner-bean");
        div.setText(context.getBean(NoOwnerBean.class).getValue());
        add(div);

        buttonScopedBeanCount = new Div();
        buttonScopedBeanCount.setId("button-scoped-bean-count");
        add(buttonScopedBeanCount);
    }

    @Override
    public void add(Component... components) {
        Stream.of(components).forEach(
                comp -> comp.getElement().getStyle().set("display", "block"));
        super.add(components);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        buttonScopedBeanCount.setText(String.valueOf(
                context.getBeansOfType(ButtonScopedBean.class).size()));
    }

}
