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
package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RefreshCloseConnectionIT extends ChromeBrowserTest {

    @Test
    public void sessionRefresh() {
        String param = UUID.randomUUID().toString();
        open(param);

        if (hasClientIssue("7587")) {
            return;
        }

        waitUntil(driver -> getLastLog() != null);
        Assert.assertEquals("Init", getLastLog());

        open(param);

        waitUntil(driver -> getLastLog() != null);

        List<WebElement> logs = findElements(By.className("log"));
        Set<String> set = logs.stream().map(element -> element.getText())
                .collect(Collectors.toSet());

        Assert.assertTrue(set.contains("Refresh"));
        Assert.assertTrue(set.contains("Push"));
    }

    private String getLastLog() {
        List<WebElement> logs = findElements(By.className("log"));
        if (logs.isEmpty()) {
            return null;
        }
        return logs.get(logs.size() - 1).getText();
    }
}
