/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package org.vaadin.example;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

/**
 * Class for testing reload time of tiny Vaadin app triggered by spring-boot Dev
 * Tool.
 */
public class SpringDevToolsReloadViewIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/reload-test";
    }

    @Test
    public void testSpringBootReloadTime() {
        open();

        waitForElementPresent(By.id("start-button"));

        findElement(By.id("start-button")).click(); // trigger for reload

        waitForElementVisible(By.id("result"));

        String reloadTimeInMsText = findElement(By.id("result")).getText();
        Assert.assertNotNull(reloadTimeInMsText);

        String reloadTimeInMs = reloadTimeInMsText.substring(
                reloadTimeInMsText.indexOf("[") + 1,
                reloadTimeInMsText.indexOf("]"));

        System.out.printf(
                "##teamcity[buildStatisticValue key='small,spring-boot-devtools-reload-time' value='%s']%n",
                reloadTimeInMs);
    }

}
