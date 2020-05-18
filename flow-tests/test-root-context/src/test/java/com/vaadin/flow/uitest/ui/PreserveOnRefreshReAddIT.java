/*
 * Copyright 2000-2020 Vaadin Ltd.
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

public class PreserveOnRefreshReAddIT extends ChromeBrowserTest {

    @Test
    public void replaceComponentAfterRefresh_componentIsReplaced() {
        open();
        if (hasClientIssue("7587")) {
            return;
        }

        findElement(By.id("set-text")).click();
        findElement(By.id("set-another-text")).click();

        WebElement container = findElement(By.id("container"));

        // self check
        Assert.assertEquals("Another Text", container.getText());

        open();

        findElement(By.id("set-text")).click();

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
        container = findElement(By.id("container"));
        Assert.assertEquals("Text", container.getText());
    }
}
