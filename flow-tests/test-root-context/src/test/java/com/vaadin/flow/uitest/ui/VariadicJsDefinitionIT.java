/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class VariadicJsDefinitionIT extends ChromeBrowserTest {

    @Test
    public void trailingArguments_collectedByTheGeneratedFunction() {
        open();

        findElement(By.id("call-many")).click();

        // Each argument reaches the browser on its own, and the generated
        // function collects those that follow the fixed one
        Assert.assertEquals("a-1-true-b", getResult());
    }

    @Test
    public void noTrailingArguments_functionCalledWithTheFixedOnes() {
        open();

        findElement(By.id("call-none")).click();

        Assert.assertEquals("a", getResult());
    }

    private String getResult() {
        waitUntil(driver -> !findElement(By.id("result")).getText().isEmpty());
        return findElement(By.id("result")).getText();
    }
}
