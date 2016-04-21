/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;
import com.vaadin.testbench.By;

public class WaitForVaadinIT extends PhantomJSTest {
    @Test
    public void testWaitForVaadin() {
        open();

        WebElement message = findElement(By.id("message"));
        WebElement button = findElement(By.tagName("button"));

        Assert.assertEquals("Not updated", message.getText());

        button.click();
        testBench().waitForVaadin();

        Assert.assertEquals("Updated", message.getText());
    }
}
