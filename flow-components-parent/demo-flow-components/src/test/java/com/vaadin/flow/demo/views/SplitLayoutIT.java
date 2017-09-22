/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo.views;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.demo.ComponentDemoTest;

/**
 * Integration tests for {@link SplitLayoutView}.
 */
public class SplitLayoutIT extends ComponentDemoTest {
    private static String SPLIT_LAYOUT_TAG = "vaadin-split-layout";

    @Override
    protected String getTestPath() {
        return "/vaadin-split-layout";
    }

    @Test
    public void combined_layouts() {
        WebElement splitLayout = layout
                .findElements(By.tagName(SPLIT_LAYOUT_TAG)).get(2);
        WebElement firstComponent = splitLayout
                .findElement(By.id("first-component"));
        WebElement nestedLayout = splitLayout.findElement(By.id("nested-layout"));
        WebElement secondComponent = nestedLayout
                .findElement(By.id("second-component"));
        WebElement thirdComponent = nestedLayout
                .findElement(By.id("third-component"));

        Assert.assertTrue("First component on the left", firstComponent
                .getLocation().x < secondComponent.getLocation().x
                && firstComponent.getLocation().x < thirdComponent
                        .getLocation().x);
        Assert.assertTrue("Second component above third component",
                secondComponent.getLocation().y < thirdComponent
                        .getLocation().y);
    }

    @Test
    @Ignore // Due to drag and drop issues with selenium.
    public void resize_events_fired() {
        WebElement splitLayout = layout
                .findElements(By.tagName(SPLIT_LAYOUT_TAG)).get(3);
        WebElement resizeMessage = layout
                .findElement(By.id("resize-message"));
        WebElement splitter = getInShadowRoot(splitLayout, By.id("splitter"))
                .findElement(By.tagName("div"));

        new Actions(getDriver()).dragAndDropBy(splitter, 1, 1).clickAndHold()
                .moveByOffset(200, 0).release().build().perform();

        Assert.assertTrue("Resize events fired",
                resizeMessage.getText().matches("Resized \\d+ times."));
        Assert.assertTrue("More than 1 resize events fired",
                !resizeMessage.getText().matches("Resized 1 times."));
    }

    @Test
    public void initial_splitter_position() {
        WebElement primaryComponent = findElement(
                By.id("initial-sp-first-component"));
        WebElement secondaryComponent = findElement(
                By.id("initial-sp-second-component"));

        Assert.assertTrue("Primary component should take up ~80% space",
                Math.abs(((float) primaryComponent.getSize().width)
                        / ((float) secondaryComponent.getSize().width)
                        - 4) < 0.1);
    }

    @Test
    @Ignore // Due to drag and drop issues with selenium.
    public void min_and_max_width_splitter() {
        WebElement splitLayout = layout
                .findElements(By.tagName(SPLIT_LAYOUT_TAG)).get(5);
        WebElement splitter = getInShadowRoot(splitLayout, By.id("splitter"))
                .findElement(By.tagName("div"));
        WebElement primaryComponent = findElement(
                By.id("min-max-first-component"));

        new Actions(getDriver()).moveToElement(splitter, 1, 1).clickAndHold()
                .moveByOffset(-1000, 0).release().build().perform();

        Assert.assertEquals("Primary component width should be 100",
                100, primaryComponent.getSize().width);

        new Actions(getDriver()).moveToElement(splitter, 1, 1).clickAndHold()
                .moveByOffset(2000, 0).release().build().perform();

        Assert.assertEquals("Primary component width should be 150",
                150, primaryComponent.getSize().width);
    }
}
