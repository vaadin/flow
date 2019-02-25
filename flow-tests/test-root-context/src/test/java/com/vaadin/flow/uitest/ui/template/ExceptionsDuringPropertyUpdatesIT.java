/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
