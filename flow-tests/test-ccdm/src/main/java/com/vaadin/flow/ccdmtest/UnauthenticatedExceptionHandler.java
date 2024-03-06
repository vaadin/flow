/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.HttpStatusCode;

@Tag(Tag.DIV)
public class UnauthenticatedExceptionHandler extends Component
        implements HasErrorParameter<UnauthenticatedException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<UnauthenticatedException> parameter) {
        setId("errorView");
        getElement().setText(
                "Tried to navigate to a view without being authenticated");
        return HttpStatusCode.UNAUTHORIZED.getCode();
    }
}
