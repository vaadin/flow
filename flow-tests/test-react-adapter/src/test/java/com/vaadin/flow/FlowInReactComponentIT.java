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
package com.vaadin.flow;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.FlowInReactComponentView.ADD_MAIN;
import static com.vaadin.flow.FlowInReactComponentView.ADD_SECONDARY;
import static com.vaadin.flow.FlowInReactComponentView.REMOVE_MAIN;
import static com.vaadin.flow.FlowInReactComponentView.REMOVE_SECONDARY;
import static com.vaadin.flow.ReactLayout.MAIN_CONTENT;
import static com.vaadin.flow.ReactLayout.SECONDARY_CONTENT;

public class FlowInReactComponentIT extends ChromeBrowserTest {

    @Test
    public void validateComponentPlacesAndFunction() {
        open();

        waitForDevServer();

        Assert.assertTrue("No react component displayed",
                $("react-layout").first().isDisplayed());

        List<WebElement> list = $("react-layout").first()
                .findElements(By.xpath("./child::*"));
        Assert.assertEquals("React component child count wrong", list.size(),
                6);

        Assert.assertEquals("span", list.get(0).getTagName());
        Assert.assertEquals("flow-content-container", list.get(1).getTagName());
        Assert.assertEquals("div", list.get(2).getTagName());
        Assert.assertEquals("span", list.get(3).getTagName());
        Assert.assertEquals("div", list.get(4).getTagName());
        Assert.assertEquals("flow-content-container", list.get(5).getTagName());

        TestBenchElement content = $("react-layout").first()
                .findElement(By.name(MAIN_CONTENT));
        TestBenchElement secondary = $("react-layout").first()
                .findElement(By.name(SECONDARY_CONTENT));

        list = content.findElements(By.xpath("./child::*"));
        Assert.assertEquals("Flow content container count wrong", list.size(),
                3);

        $(NativeButtonElement.class).id(ADD_MAIN).click();
        Assert.assertEquals(1, content.$(DivElement.class).all().size());

        list = $("react-layout").first().findElements(By.xpath("./child::*"));
        Assert.assertEquals(
                "Adding flow component should not add to main react component",
                list.size(), 6);

        list = secondary.findElements(By.xpath("./child::*"));
        Assert.assertEquals(
                "Adding flow component should not add to secondary flow content",
                list.size(), 3);

        list = content.findElements(By.xpath("./child::*"));
        Assert.assertEquals("Flow content container count wrong", list.size(),
                4);

        $(NativeButtonElement.class).id(ADD_MAIN).click();
        Assert.assertEquals(2, content.$(DivElement.class).all().size());
        $(NativeButtonElement.class).id(ADD_MAIN).click();
        Assert.assertEquals(3, content.$(DivElement.class).all().size());

        $(NativeButtonElement.class).id(REMOVE_MAIN).click();
        Assert.assertEquals(2, content.$(DivElement.class).all().size());
        $(NativeButtonElement.class).id(REMOVE_MAIN).click();
        Assert.assertEquals(1, content.$(DivElement.class).all().size());
        $(NativeButtonElement.class).id(REMOVE_MAIN).click();
        Assert.assertEquals(0, content.$(DivElement.class).all().size());

        $(NativeButtonElement.class).id(ADD_SECONDARY).click();
        Assert.assertEquals(1, secondary.$(DivElement.class).all().size());

        list = content.findElements(By.xpath("./child::*"));
        Assert.assertEquals("Flow content container count wrong", list.size(),
                3);

        list = $("react-layout").first().findElements(By.xpath("./child::*"));
        Assert.assertEquals(
                "Adding flow component should not add to main react component",
                list.size(), 6);

        $(NativeButtonElement.class).id(REMOVE_SECONDARY).click();
        Assert.assertEquals(0, secondary.$(DivElement.class).all().size());
    }

}
