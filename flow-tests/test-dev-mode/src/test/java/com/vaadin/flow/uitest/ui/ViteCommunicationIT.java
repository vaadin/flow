/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ViteCommunicationIT extends ChromeBrowserTest {

    @Test
    public void messageSentToViteAndBack() {
        open();
        $(NativeButtonElement.class).id("send").click();
        waitUntil(driver -> $(DivElement.class).id("response").getText().equals(
                "Got event test-event-response with data {\"foo\":\"bar\"}"));
    }

}
