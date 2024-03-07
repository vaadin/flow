/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

@Route("view-throws-exception")
public class ViewThrowsException extends Div implements HasDynamicTitle {

    public ViewThrowsException() {
        Span textField = new Span(
                "You should not see this page, you cannot go back to the main page");

        add(textField);
    }

    @Override
    public String getPageTitle() {
        // Use backend information
        throw new UnauthenticatedException();
    }
}
