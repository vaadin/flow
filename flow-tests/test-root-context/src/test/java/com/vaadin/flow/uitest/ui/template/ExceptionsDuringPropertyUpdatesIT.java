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
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.uitest.ui.AbstractErrorIT;
import com.vaadin.testbench.TestBenchElement;

public class ExceptionsDuringPropertyUpdatesIT extends AbstractErrorIT {

    @Test
    public void exceptionInMapSyncDoesNotCauseInternalError() {
        open();

        TestBenchElement template = $("exceptions-property-update").first();

        template.$("button").id("set-properties").click();

        assertNoSystemErrors();

        TestBenchElement msg = template.$("div").id("message");

        Assert.assertEquals("Name is updated to bar", msg.getText());

        List<TestBenchElement> errors = template.$("div")
                .attribute("class", "error").all();

        Set<String> errorMsgs = errors.stream().map(TestBenchElement::getText)
                .collect(Collectors.toSet());

        Assert.assertEquals(2, errorMsgs.size());

        Assert.assertTrue(errorMsgs.contains(
                "An error occurred: java.lang.RuntimeException: Intentional exception in property sync handler for 'text'"));
        Assert.assertTrue(errorMsgs.contains(
                "An error occurred: java.lang.IllegalStateException: Intentional exception in property sync handler for 'title'"));
    }

}
