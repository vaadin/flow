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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class JsFunctionIT extends ChromeBrowserTest {

    @Test
    public void capturedValuesArePreBound() {
        open();
        findElement(By.id("captureButton")).click();
        WebElement result = waitUntil(d -> findElement(By.id("captureResult")));
        Assert.assertEquals("Hello World", result.getText());
    }

    @Test
    public void runtimeArgumentsAreForwarded() {
        open();
        findElement(By.id("argsButton")).click();
        WebElement result = waitUntil(d -> findElement(By.id("argsResult")));
        Assert.assertEquals("alpha:beta", result.getText());
    }

    @Test
    public void elementCaptureResolvesToDomNode() {
        open();
        findElement(By.id("elementCaptureButton")).click();
        WebElement result = waitUntil(
                d -> findElement(By.id("elementCaptureResult")));
        Assert.assertEquals("mutated via capture", result.getText());
    }

    @Test
    public void callerControlsThisBinding() {
        open();
        findElement(By.id("thisRespectButton")).click();
        WebElement result = waitUntil(
                d -> findElement(By.id("thisRespectResult")));
        Assert.assertEquals("this is the host element", result.getText());
    }
}
