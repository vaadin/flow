/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;

/*
 * Note that @Push is generally not supported in this location, but instead
 * explicitly picked up by logic in the BasicPushUI constructor.
 */
@Push
public class BasicPushUI extends ClientServerCounterUI {
    @Override
    protected void init(VaadinRequest request) {
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        Push push = getClass().getAnnotation(Push.class);

        getPushConfiguration().setPushMode(push.value());
        getPushConfiguration().setTransport(push.transport());

        super.init(request);
    }
}
