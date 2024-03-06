/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

public class RoutedSubContextSubPathIT extends RoutedSubContextIT {

    @Override
    protected String getAppContext() {
        return "/routed/sub-context/foo/bar";
    }

}
