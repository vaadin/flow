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
package com.vaadin.flow.uitest.ui.dependencies;

import java.util.List;
import java.util.logging.Level;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DynamicDependencyIT extends ChromeBrowserTest {

    @Test
    public void dynamicDependencyIsExecutedBeforeOtherMessageProcessing() {
        open();

        WebElement depElement = findElement(By.id("dep"));
        // true means that the added component (a new one) is not yet in the DOM
        Assert.assertEquals(Boolean.TRUE.toString(), depElement.getText());
    }

    @Test
    public void dependecyIsNoPromise_errorLogged() {
        testErrorCase("nopromise", "result is not a Promise");
    }

    @Test
    public void dependecyLoaderThrows_errorLogged()
            throws InterruptedException {
        testErrorCase("throw", "Throw on purpose");
    }

    @Test
    public void dependecyLoaderRejects_errorLogged()
            throws InterruptedException {
        testErrorCase("reject", "Reject on purpose");
    }

    private void testErrorCase(String caseName, String errorMessageSnippet) {
        open();

        findElement(By.id(caseName)).click();

        String statusText = findElement(By.id("new-component")).getText();
        Assert.assertEquals("Div updated for " + caseName, statusText);

        List<LogEntry> entries = getLogEntries(Level.SEVERE);
        Assert.assertEquals(2, entries.size());

        Assert.assertThat(entries.get(0).getMessage(),
                Matchers.containsString(errorMessageSnippet));
        Assert.assertThat(entries.get(1).getMessage(),
                Matchers.containsString("could not be loaded"));
    }
}
