/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring.test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Async;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

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
