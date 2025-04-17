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
package com.vaadin.flow.uitest.ui.template;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Tests to validate the ordering of server-side nodes when added alongside
 * client-side nodes.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ChildOrderIT extends ChromeBrowserTest {

    private TestBenchElement root;

    @Before
    public void init() {
        open();
        waitForElementPresent(By.id("root"));
        root = $(TestBenchElement.class).id("root");
    }

    @Test
    public void appendElementsFromServer_elementsAreAddedAfterExistingOnes() {
        TestBenchElement container = root.$(TestBenchElement.class)
                .id("containerWithElement");

        assertNodeOrder(container, "Client child");

        clickAndWaitForContainerToChange(container, "addChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 1");

        clickAndWaitForContainerToChange(container, "addChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 1",
                "Server child 2");

        clickAndWaitForContainerToChange(container,
                "addClientSideChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 1",
                "Server child 2", "Client child");

        /*
         * Client side nodes added after the server side ones are not considered
         * in the counting, so they are left behind
         */
        clickAndWaitForContainerToChange(container, "addChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 1",
                "Server child 2", "Server child 3", "Client child");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer1");
        assertNodeOrder(container, "Client child", "Server child 1",
                "Server child 2", "Client child");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer1");
        assertNodeOrder(container, "Client child", "Server child 1",
                "Client child");
    }

    @Test
    public void prependElementsFromServer_elementsAreAddedBeforeExistingOnes() {
        TestBenchElement container = root.$(TestBenchElement.class)
                .id("containerWithElement");

        assertNodeOrder(container, "Client child");

        clickAndWaitForContainerToChange(container, "prependChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 1");

        clickAndWaitForContainerToChange(container, "prependChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 2",
                "Server child 1");

        clickAndWaitForContainerToChange(container,
                "addClientSideChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 2",
                "Server child 1", "Client child");

        clickAndWaitForContainerToChange(container, "prependChildToContainer1");
        assertNodeOrder(container, "Client child", "Server child 3",
                "Server child 2", "Server child 1", "Client child");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer1");
        assertNodeOrder(container, "Client child", "Server child 3",
                "Server child 2", "Client child");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer1");
        assertNodeOrder(container, "Client child", "Server child 3",
                "Client child");
    }

    @Test
    public void appendTextsFromServer_textsAreAddedAfterExistingOnes() {
        TestBenchElement container = root.$(TestBenchElement.class)
                .id("containerWithText");

        assertNodeOrder(container, "Client text");

        clickAndWaitForContainerToChange(container, "addChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 1");

        clickAndWaitForContainerToChange(container, "addChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 1",
                "Server text 2");

        clickAndWaitForContainerToChange(container,
                "addClientSideChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 1",
                "Server text 2", "Client text");

        /*
         * Client side nodes added after the server side ones are not considered
         * in the counting, so they are left behind
         */
        clickAndWaitForContainerToChange(container, "addChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 1",
                "Server text 2", "Server text 3", "Client text");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer2");
        assertNodeOrder(container, "Client text", "Server text 1",
                "Server text 2", "Client text");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer2");
        assertNodeOrder(container, "Client text", "Server text 1",
                "Client text");
    }

    @Test
    public void prependTextsFromServer_textsAreAddedBeforeExistingOnes() {
        TestBenchElement container = root.$(TestBenchElement.class)
                .id("containerWithText");

        assertNodeOrder(container, "Client text");

        clickAndWaitForContainerToChange(container, "prependChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 1");

        clickAndWaitForContainerToChange(container, "prependChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 2",
                "Server text 1");

        clickAndWaitForContainerToChange(container,
                "addClientSideChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 2",
                "Server text 1", "Client text");

        clickAndWaitForContainerToChange(container, "prependChildToContainer2");
        assertNodeOrder(container, "Client text", "Server text 3",
                "Server text 2", "Server text 1", "Client text");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer2");
        assertNodeOrder(container, "Client text", "Server text 3",
                "Server text 2", "Client text");

        clickAndWaitForContainerToChange(container,
                "removeChildFromContainer2");
        assertNodeOrder(container, "Client text", "Server text 3",
                "Client text");
    }

    @Test
    public void containerWithElementAddedOnConstructor_orderIsPreserved() {
        TestBenchElement container = root.$(TestBenchElement.class)
                .id("containerWithElementAddedOnConstructor");

        assertNodeOrder(container, "Client child", "Server child 1",
                "Server child 2");
    }

    private void clickAndWaitForContainerToChange(WebElement container,
            String buttonToclick) {
        String innertText = container.getAttribute("innerText");
        TestBenchElement button = root.$(TestBenchElement.class)
                .id(buttonToclick);
        button.click();
        waitUntilNot(driver -> container.getAttribute("innerText")
                .equals(innertText));
    }

    private void assertNodeOrder(WebElement container, String... nodes) {
        String texts = container.getText();
        texts = texts.replace("\n", " ");
        String expected = Stream.of(nodes).collect(Collectors.joining(" "));
        Assert.assertEquals(expected, texts);
    }

}
