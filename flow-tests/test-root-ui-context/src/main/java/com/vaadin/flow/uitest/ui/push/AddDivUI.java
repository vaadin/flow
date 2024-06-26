/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;

@Push
public class AddDivUI extends UI {

    private int msgId = 1;
    private String ip;

    @Override
    protected void init(VaadinRequest request) {
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        Push push = getClass().getAnnotation(Push.class);

        getPushConfiguration().setPushMode(push.value());
        getPushConfiguration().setTransport(push.transport());

        ip = request.getRemoteAddr();
        addDiv();

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        long delay = 10;
        ses.scheduleAtFixedRate(() -> {

            access(() -> {
                addDiv();
            });

            if (msgId > 500) {
                throw new RuntimeException("Done");
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    private void addDiv() {
        Element bodyElement = getElement();
        Element div = ElementFactory.createDiv("Hello world at "
                + System.currentTimeMillis() + " (" + msgId++ + ")");
        bodyElement.insertChild(0, div);
        if (msgId % 100 == 0) {
            System.out.println("Pushed id " + msgId + " to " + ip);
        }
        // FIXME Enable when remove works
        // while (bodyElement.getChildCount() > 20) {
        // bodyElement.removeChild(20);
        // }
    }
}
