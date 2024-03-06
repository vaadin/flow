/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

public class SubContextSubPathIT extends SubContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context/foo/bar";
    }

}
