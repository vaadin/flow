/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template.collections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ClearListIT extends ChromeBrowserTest {

    @Test
    public void checkThatListCanBeClearedWithModelHavingNoDefaultConstructor() {
        checkThatModelHasNoDefaultConstructor();
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        List<String> initialMessages = getMessages(template);

        Assert.assertEquals("Initial page does not contain expected messages",
                Arrays.asList("1", "2"), initialMessages);

        template.$(TestBenchElement.class).id("clearList").click();

        Assert.assertTrue(
                "Page should not contain elements after we've cleared them",
                getMessages(template).isEmpty());
    }

    private void checkThatModelHasNoDefaultConstructor() {
        Constructor<?>[] modelConstructors = ClearListView.Message.class
                .getConstructors();
        Assert.assertEquals("Expect model to have one constructor exactly", 1,
                modelConstructors.length);
        Assert.assertTrue(
                "Expect model to have at least one parameter in its single constructor",
                modelConstructors[0].getParameterCount() > 0);
    }

    private List<String> getMessages(TestBenchElement template) {
        return template.$(TestBenchElement.class).attribute("class", "msg")
                .all().stream().map(WebElement::getText)
                .collect(Collectors.toList());
    }
}
