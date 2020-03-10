package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;

public abstract class AbstractTestUIWithLog extends UI {

    private Log log = new Log();

    @Override
    protected void init(VaadinRequest request) {
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        Push push = getClass().getAnnotation(Push.class);

        if (push != null) {
            getPushConfiguration().setPushMode(push.value());
            getPushConfiguration().setTransport(push.transport());
        }

        add(log);
    }

    protected void log(String msg) {
        log.log(msg);
    }
}
