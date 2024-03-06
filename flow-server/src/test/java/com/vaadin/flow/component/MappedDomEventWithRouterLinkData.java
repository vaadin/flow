/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.router.RouterLink;

public class MappedDomEventWithRouterLinkData
        extends MappedDomEventWithComponentData {

    private final RouterLink routerLink;

    public MappedDomEventWithRouterLinkData(Component source,
            boolean fromClient, @EventData("component") Component component,
            @EventData("router.link") RouterLink routerLink) {
        super(source, fromClient, component);
        this.routerLink = routerLink;
    }

    public RouterLink getRouterLink() {
        return routerLink;
    }
}
