/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.internal.DefaultErrorHandler;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * This is a default error view shown on access denied routing exceptions.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
@AnonymousAllowed
@DefaultErrorHandler
public class RouteAccessDeniedError extends Component
        implements HasErrorParameter<AccessDeniedException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<AccessDeniedException> parameter) {
        event.rerouteToError(NotFoundException.class,
                parameter.getCustomMessage());
        return HttpStatusCode.NOT_FOUND.getCode();
    }
}
