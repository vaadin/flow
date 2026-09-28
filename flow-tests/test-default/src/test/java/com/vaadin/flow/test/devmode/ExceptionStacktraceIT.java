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
package com.vaadin.flow.test.devmode;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(ExceptionStacktraceView.class)
public class ExceptionStacktraceIT extends AbstractDefaultIT {

    @BrowserTest
    public void loggerAbsenceWarningAndStacktrace() {
        open();

        WebElement main = findElements(By.cssSelector("body > div")).stream()
                .filter(element -> element.getDomAttribute("class") == null
                        || element.getDomAttribute("class").isEmpty())
                .findFirst().orElseThrow();

        assertFalse(
                findElements(By.cssSelector("body > div > div")).stream()
                        .anyMatch(div -> div.getText().toUpperCase()
                                .contains("SLF4J")),
                "There should be no warning about SLF4J absence because the test project should have slf4j bindings");

        String stacktrace = main.findElement(By.tagName("pre")).getText();

        // The whole chain is expected on the page: the exception the
        // instantiator throws when the view cannot be created (a Spring
        // BeanCreationException in this module, since views are beans) and the
        // cause thrown by the view constructor.
        assertTrue(stacktrace.contains("BeanCreationException"),
                "There is no top level exception in the stacktrace: "
                        + stacktrace);
        assertTrue(
                stacktrace.contains(
                        "Caused by: java.lang.RuntimeException: Error here!"),
                "There is no exception cause in the stacktrace: " + stacktrace);
    }
}
