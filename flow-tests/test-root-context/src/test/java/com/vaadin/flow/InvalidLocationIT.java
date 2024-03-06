/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Test;

public class InvalidLocationIT extends ChromeBrowserTest {

    // #9443
    @Test
    public void invalidCharactersOnPath_UiNotServed() {
        open();

        checkLogsForErrors(msg -> msg
                .contains("the server responded with a status of 400"));
    }

    @Override
    protected String getTestPath() {
        return "/view/..**";
    }
}
