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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateEventHandlerIT extends PhantomJSTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void executeEventHandler() {
        findElement(By.id("event-receiver")).click();

        Assert.assertTrue(isElementPresent(By.id("event-handler")));
    }

    @Test
    public void sendArgumentsToServer() {
        findElement(By.id("arg-receiver")).click();

        Assert.assertTrue(isElementPresent(By.id("event-handler")));
    }
}
