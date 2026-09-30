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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientEventBusIT extends ChromeBrowserTest {

    @Test
    public void clickButton_busFiresStartResponseAndEndOfRequest() {
        open();
        // The listener is added while a response is being handled, so it may
        // already have seen the end of that request.
        int linesBeforeClick = getLogLines().size();

        findElement(By.id("send")).click();
        waitUntil(driver -> getLogLines().size() >= linesBeforeClick + 3);

        List<String> allLines = getLogLines();
        List<String> lines = allLines.subList(linesBeforeClick,
                allLines.size());
        String requestId = lines.get(0)
                .substring("vaadin-request-start ".length());
        Assert.assertEquals(List.of("vaadin-request-start " + requestId,
                "vaadin-response-start " + requestId,
                "vaadin-request-end " + requestId), lines);
        checkLogsForErrors();
    }

    private List<String> getLogLines() {
        return findElement(By.id("log")).findElements(By.className("log"))
                .stream().map(WebElement::getText).toList();
    }
}
