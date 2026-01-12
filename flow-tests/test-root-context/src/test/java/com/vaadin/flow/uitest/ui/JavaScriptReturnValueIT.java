/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class JavaScriptReturnValueIT extends ChromeBrowserTest {
    @Test
    public void testAllCombinations() {
        open();

        /*
         * There are 3 * 3 * 2 * 3 = 54 different combinations in the UI, let's
         * test all of them just because we can
         */
        for (String method : Arrays.asList("execPage", "execElement",
                "callElement")) {
            for (String value : Arrays.asList("string", "number", "null")) {
                for (String outcome : Arrays.asList("success", "failure")) {
                    for (String type : Arrays.asList("synchronous",
                            "resolvedpromise", "timeout")) {
                        testCombination(method, value, outcome, type);
                    }
                }
            }
        }
    }

    private void testCombination(String method, String value, String outcome,
            String type) {
        String combinationId = String.join(", ", method, value, outcome, type);
        String expectedStatus = getExpectedStatus(value, outcome);

        for (String target : Arrays.asList("clear", method, value, outcome,
                type, "run")) {
            findElement(By.id(target)).click();
        }

        if ("timeout".equals(type)) {
            try {
                Assert.assertEquals(
                        "Result should not be there immediately for "
                                + combinationId,
                        "Running...", findElement(By.id("status")).getText());

                waitUntil(ExpectedConditions.textToBe(By.id("status"),
                        expectedStatus), 2);
            } catch (TimeoutException e) {
                Assert.fail("Didn't reach expected result for " + combinationId
                        + ". Expected " + expectedStatus + " but got "
                        + findElement(By.id("status")).getText());
                e.printStackTrace();
            }
        } else {
            String actualStatus = findElement(By.id("status")).getText();
            Assert.assertEquals("Unexpected result for " + combinationId,
                    expectedStatus, actualStatus);
        }
    }

    private String getExpectedStatus(String value, String outcome) {
        String prefix = "";
        if ("failure".equals(outcome)) {
            prefix = "Error: ";

            if ("null".equals(value)) {
                // Special case since the null is handled differently for errors
                // and for results
                return prefix + "null";
            }
        }

        switch (value) {
        case "string":
            return prefix + "foo";
        case "number":
            return prefix + "42";
        case "null":
            return prefix;
        default:
            throw new IllegalArgumentException(
                    "Unsupported value type: " + value);
        }
    }

}
