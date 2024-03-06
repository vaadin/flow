/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@RouteScope
@RouteScopeOwner(CustomExceptionRoute.class)
@Component
public class CustomExceptionBean implements DisposableBean {

    private Registration listener;

    @PostConstruct
    private void postConstruct() {
        listener = UI.getCurrent().addAfterNavigationListener(event -> {
            Div div = new Div();
            div.setId("custom-exception-created");
            div.setText("custom exception bean is created");
            HasElement hasElement = event.getActiveChain().get(0);
            hasElement.getElement().appendChild(div.getElement());
            listener.remove();
        });
    }

    @Override
    public void destroy() throws Exception {
        listener = UI.getCurrent().addAfterNavigationListener(event -> {
            Div div = new Div();
            div.setId("custom-exception-destroyed");
            div.setText("custom exception bean is destroyed");
            HasElement hasElement = event.getActiveChain().get(0);
            hasElement.getElement().appendChild(div.getElement());
            listener.remove();
        });
    }

}
