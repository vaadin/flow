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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.webcomponent.FireEventComponent.OptionsType.Bubble_Cancel;
import static com.vaadin.flow.webcomponent.FireEventComponent.OptionsType.Bubble_NoCancel;
import static com.vaadin.flow.webcomponent.FireEventComponent.OptionsType.NoBubble_NoCancel;

public class FireEventIT extends ChromeBrowserTest {
    private static final String N1 = "number1";
    private static final String N2 = "number2";
    private static final String SUM = "sum";
    private static final String ERR = "error";
    private static final String OUT_RESULT = "outer-result";
    private static final String IN_RESULT = "inner-result";
    private static final String CON_RESULT = "contained-result";

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/fireEvent.html";
    }

    @Test
    public void customEventsGetSentToTheClientSide() {
        open();

        waitForElementVisible(By.id("calc"));

        WebElement calc = findElement(By.id("calc"));

        WebElement button = calc.findElement(By.id("button"));
        WebElement number1 = calc.findElement(By.id(N1));
        WebElement number2 = calc.findElement(By.id(N2));

        Assert.assertEquals("Sum should be 0", "0", value(SUM));
        Assert.assertEquals("Error should be empty", "", value(ERR));

        number1.sendKeys("4.5", Keys.ENTER);
        number2.sendKeys("3.5", Keys.ENTER);

        button.click();

        Assert.assertEquals("Sum should be 8", "8", value(SUM));
        Assert.assertEquals("Error should be empty", "", value(ERR));

        number1.clear();
        number2.clear();

        Assert.assertEquals("", value(N1));
        Assert.assertEquals("", value(N2));

        button.click();

        Assert.assertEquals("Sum should not have changed", "8", value(SUM));
        Assert.assertEquals("Error should have been raised", "empty String",
                value(ERR));

        number1.sendKeys("99", Keys.ENTER);
        number2.sendKeys("101", Keys.ENTER);

        button.click();

        Assert.assertEquals("Sum should be 200", "200", value(SUM));
    }

    @Test
    public void options_bubblesAndCancelableAreRecordedOntoTheEventAndWork() {
        open();

        waitForElementVisible(By.id("contained"));

        /*
            Inner-div listener attempts to cancel all button-events
         */
        WebElement contained = findElement(By.id("contained"));
        // non-bubbling
        WebElement button1 = contained.findElement(By.id("b1"));
        // bubbling, non-cancelable
        WebElement button2 = contained.findElement(By.id("b2"));
        // bubbling, cancellable
        WebElement button3 = contained.findElement(By.id("b3"));

        button1.click();

        Assert.assertEquals("Non-bubbling event should be visible on the " +
                "web component", NoBubble_NoCancel.name(), value(CON_RESULT));
        Assert.assertEquals("Non-bubbling event should not be visible on the " +
                "inner div", "", value(IN_RESULT));
        Assert.assertEquals("Non-bubbling event should not be visible on the " +
                "outer div", "", value(OUT_RESULT));

        button2.click();

        Assert.assertEquals("Bubbling, non-cancellable event should be " +
                "visible on the web component", Bubble_NoCancel.name(),
                value(CON_RESULT));
        Assert.assertEquals("Bubbling, non-cancelable event should be visible on the " +
                "inner div", Bubble_NoCancel.name(), value(IN_RESULT));
        Assert.assertEquals("Bubbling, non-cancelable event should be visible on the " +
                "outer div", Bubble_NoCancel.name(), value(OUT_RESULT));

        button3.click();

        Assert.assertEquals("Bubbling, cancellable event should be " +
                "visible on the web component", Bubble_Cancel.name(),
                value(CON_RESULT));
        Assert.assertEquals("Bubbling, cancelable event should be visible on the " +
                "inner div", Bubble_Cancel.name(), value(IN_RESULT));
        /*
            Since we cannot actually stop CustomEvents from being actuated by
             custom event listeners, we can only track the flag
             "defaultPrevented" to verify that everything is as it should be.
         */
        Assert.assertEquals("Bubbling, cancelable event should not be visible on the " +
                "outer div", "prevented", value(OUT_RESULT));
    }

    private String value(String id) {
        return findElement(By.id(id)).getText();
    }
}
