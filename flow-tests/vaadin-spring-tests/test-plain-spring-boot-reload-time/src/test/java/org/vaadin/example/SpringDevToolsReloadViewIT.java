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

import com.vaadin.flow.server.Version;
import com.vaadin.flow.spring.test.SpringDevToolsReloadUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Class for testing reload time of tiny Spring app triggered by spring-boot Dev
 * Tool.
 */
public class SpringDevToolsReloadViewIT extends ChromeBrowserTest {

    @Test
    public void testPlainSpringBootReloadTime_withHelloWorld() {
        String result = SpringDevToolsReloadUtils
                .runAndCalculateAverageResult(5, () -> {
                    open("/");

                    waitUntil(ExpectedConditions.presenceOfElementLocated(
                            By.id("start-button")), 10);
                    triggerReload();
                    waitUntil(ExpectedConditions
                            .visibilityOfElementLocated(By.id("result")), 10);

                    return assertAndGetReloadTimeResult();
                });

        System.out.printf(
                "##teamcity[buildStatisticValue key='%s,plain-spring-hello-world,spring-boot-devtools-reload-time' value='%s']%n",
                getVaadinMajorMinorVersion(), result);
    }

    private void triggerReload() {
        findElement(By.id("start-button")).click(); // trigger for reload
    }

    private void open(String testPath) {
        getDriver().get(getTestURL(getRootURL(), testPath, null));
    }

    private String assertAndGetReloadTimeResult() {
        String reloadTimeInMsText = findElement(By.id("result")).getText();
        Assert.assertNotNull(reloadTimeInMsText);

        return reloadTimeInMsText.substring(reloadTimeInMsText.indexOf("[") + 1,
                reloadTimeInMsText.indexOf("]"));
    }

    private String getVaadinMajorMinorVersion() {
        return Version.getMajorVersion() + "." + Version.getMinorVersion();
    }
}
