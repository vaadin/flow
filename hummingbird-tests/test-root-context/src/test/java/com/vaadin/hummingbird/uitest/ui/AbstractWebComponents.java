/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.logging.Level;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.logging.LogEntries;

import com.vaadin.hummingbird.testutil.PhantomJSTest;
import com.vaadin.testbench.By;

/**
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractWebComponents extends PhantomJSTest {

    @Test
    public void useWebComponents() {
        open();

        Assert.assertTrue(findElements(By.tagName("script")).stream()
                .anyMatch(element -> element.getAttribute("src")
                        .contains(getWebComponentsJSFile())));

        LogEntries logs = driver.manage().logs().get("browser");
        if (logs != null) {
            Assert.assertFalse(StreamSupport.stream(logs.spliterator(), true)
                    .anyMatch(entry -> entry.getLevel().intValue() > Level.INFO
                            .intValue()));
        }
    }

    protected abstract String getWebComponentsJSFile();
}
