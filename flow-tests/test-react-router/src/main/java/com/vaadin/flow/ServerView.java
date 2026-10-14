/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

@Route("com.vaadin.flow.ServerView")
public class ServerView extends Div implements HasUrlParameter<String> {

    private final Span setParameterSpan = new Span();
    private int setParameterCount = 0;

    public ServerView() {
        NativeButton serverNavigation = new NativeButton(
                "Navigate through Server", event -> {
                    event.getSource().getUI().get()
                            .navigate(NavigationView.class);
                });
        serverNavigation.setId(NavigationView.SERVER_ID);
        setParameterSpan.setId(NavigationView.SET_PARAMETER_COUNTER_ID);

        add(new Span("ServerView"), new Div(), serverNavigation, new Div(),
                setParameterSpan);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        setParameterSpan.setText("" + ++setParameterCount);
    }
}
