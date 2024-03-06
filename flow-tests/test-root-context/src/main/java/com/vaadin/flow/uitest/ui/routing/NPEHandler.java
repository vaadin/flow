/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class NPEHandler extends Div
        implements HasErrorParameter<NullPointerException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NullPointerException> parameter) {
        getElement().setText("NPE is thrown " + event.getLocation().getPath());

        Exception exception = parameter.getCaughtException();
        LoggerFactory.getLogger(NPEHandler.class).error(exception.getMessage(),
                exception);

        setId("no-route");
        return 500;
    }

}
