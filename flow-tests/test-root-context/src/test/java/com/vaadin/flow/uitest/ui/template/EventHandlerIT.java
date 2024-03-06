/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class EventHandlerIT extends ChromeBrowserTest {

    @Test
    public void handleEventOnServer() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        template.$(TestBenchElement.class).id("handle").click();
        Assert.assertTrue(
                "Unable to find server event handler invocation confirmation. "
                        + "Looks like 'click' event handler has not been invoked on the server side",
                isElementPresent(By.id("event-handler-result")));

        template.$(TestBenchElement.class).id("send").click();
        TestBenchElement container = $(TestBenchElement.class).id("event-data");
        List<TestBenchElement> divs = container.$("div").all();

        Assert.assertEquals(
                "Unexpected 'button' event data in the received event handler parameter",
                "button: 0", divs.get(1).getText());
        Assert.assertEquals(
                "Unexpected 'type' event data in the received event handler parameter",
                "type: click", divs.get(2).getText());
        Assert.assertEquals(
                "Unexpected 'tag' event data in the received event handler parameter",
                "tag: button", divs.get(3).getText());

        // Check event functionality for event with both client and server
        // handler
        template.$(TestBenchElement.class).id("overridden").click();

        Assert.assertTrue(
                "Unable to find server event handler invocation confirmation.",
                isElementPresent(By.id("overridden-event-handler-result")));

        Assert.assertEquals("Received result wasn't updated by client!",
                "Overridden server event was invoked with result: ClientSide handler",
                findElement(By.id("overridden-event-handler-result"))
                        .getText());

        // @ClientCallable return value
        template.$("button").id("client").click();
        Assert.assertEquals("Server-side message should be present",
                "Call from client, message: foo, true",
                $("div").id("client-call").getText());
        Assert.assertEquals(
                "Message from awaiting return value should be present", "FOO",
                template.$("span").id("status").getText());

        template.$("button").id("clientError").click();
        Assert.assertEquals("Message from awaiting exception should be present",
                "Error: Something went wrong. Check server-side logs for more information.",
                template.$("span").id("status").getText());
    }
}
