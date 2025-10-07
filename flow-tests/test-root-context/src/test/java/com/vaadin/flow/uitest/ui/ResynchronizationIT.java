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

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ResynchronizationIT extends ChromeBrowserTest {

    /*
     * If a component is only added in a lost message, it should be present
     * after resynchronization.
     */
    @Test
    public void resynchronize_componentAddedInLostMessage_appearAfterResync() {
        open();

        findElement(By.id(ResynchronizationView.ADD_BUTTON)).click();

        waitForElementPresent(By.className(ResynchronizationView.ADDED_CLASS));

        findElement(By.id(ResynchronizationView.ADD_BUTTON)).click();

        waitUntil(driver -> findElements(
                By.className(ResynchronizationView.ADDED_CLASS)).size() == 2);
    }

    /*
     * If a @ClientCallable is invoked in a lost message, the promises waiting
     * for the return value from the server should be rejected rather than
     * remain pending.
     */
    @Test
    public void resynchronize_clientCallableInvoked_promisesAreRejected() {
        open();

        findElement(By.id(ResynchronizationView.CALL_BUTTON)).click();

        waitForElementPresent(
                By.className(ResynchronizationView.REJECTED_CLASS));
    }

}
