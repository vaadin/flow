/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.custom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

/**
 * Customized error handler for IllegalAccessException.
 * <p>
 * This should not be available through the CustomRouteRegistry.
 */
@Tag(Tag.DIV)
public class CustomErrorHandler extends Component
        implements HasErrorParameter<IllegalAccessException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<IllegalAccessException> parameter) {
        getElement().appendChild(
                new Span("This shouldn't be available").getElement());
        return 0;
    }
}
