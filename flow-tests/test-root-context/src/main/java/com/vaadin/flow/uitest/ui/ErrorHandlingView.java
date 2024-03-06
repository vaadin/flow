/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ErrorHandlingView", layout = ViewTestLayout.class)
public class ErrorHandlingView extends AbstractErrorHandlerView {
    public ErrorHandlingView() {
        add(createButton("Throw exception in click handler", "errorButton",
                e -> {
                    throw new IllegalStateException(
                            "Intentional fail in click handler");
                }));
        add(createButton("Throw exception in beforeClientResponse",
                "clientResponseButton", e -> {
                    e.getSource().getUI().get().getInternals().getStateTree()
                            .beforeClientResponse(getElement().getNode(),
                                    executionContext -> {
                                        throw new IllegalStateException(
                                                "Intentional fail in beforeClientResponse");
                                    });
                }));
    }

}
