/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractLiveReloadIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/context" + super.getTestPath();
    }

}
