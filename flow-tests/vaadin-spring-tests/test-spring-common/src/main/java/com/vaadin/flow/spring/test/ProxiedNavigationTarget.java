/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import static com.vaadin.flow.spring.scopes.VaadinUIScope.VAADIN_UI_SCOPE_NAME;

@Route("proxied")
public class ProxiedNavigationTarget extends Div
        implements HasUrlParameter<Integer> {

    private final String uuid = UUID.randomUUID().toString();
    private final AtomicInteger counter = new AtomicInteger();
    private final RouterLink routerLink;
    private final Div clickCounter;

    public ProxiedNavigationTarget() {
        Div uuid = new Div(this.uuid);
        uuid.setId("COMPONENT_ID");
        add(uuid);

        clickCounter = new Div("P:0, C:0");
        clickCounter.setId("CLICK_COUNTER");
        add(clickCounter);

        // Self navigation should use the same view instance
        routerLink = new RouterLink("Self Link", ProxiedNavigationTarget.class,
                counter.incrementAndGet());
        add(routerLink);
    }

    // @Async annotation should cause Spring to create a proxy for the
    // bean instance
    @Async
    public void asyncMethod() {

    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Integer parameter) {
        if (parameter != null) {
            clickCounter.setText("P:" + parameter + ", C:" + counter.get());
            routerLink.setRoute(ProxiedNavigationTarget.class,
                    counter.incrementAndGet());
        }
    }
}
