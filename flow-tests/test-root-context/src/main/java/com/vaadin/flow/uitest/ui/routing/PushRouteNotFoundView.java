/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.HttpStatusCode;

@ParentLayout(PushLayout.class)
public class PushRouteNotFoundView extends RouteNotFoundError {

    public static String PUSH_NON_EXISTENT_PATH = "push-no-route";

    private boolean isPushPath;

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        String path = event.getLocation().getPath();
        if (PUSH_NON_EXISTENT_PATH.equals(path)) {
            isPushPath = true;
            return HttpStatusCode.NOT_FOUND.getCode();
        } else {
            return super.setErrorParameter(event, parameter);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (isPushPath) {
            Element div = ElementFactory.createDiv("Push mode: "
                    + attachEvent.getUI().getPushConfiguration().getPushMode());
            div.setAttribute("id", "push-mode");
            getElement().appendChild(div);
        }
    }
}
