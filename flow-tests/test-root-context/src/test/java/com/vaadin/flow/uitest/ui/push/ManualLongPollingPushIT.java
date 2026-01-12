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
package com.vaadin.flow.uitest.ui.push;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.testbench.TestBenchElement;

@Category(PushTests.class)
public class ManualLongPollingPushIT extends AbstractLogTest {

    @Test
    public void doubleManualPushDoesNotFreezeApplication() {
        open();
        $(TestBenchElement.class).id("double-manual-push").click();
        waitUntil(textToBePresentInElement(() -> getLastLog(),
                "2. Second message logged after 1s, followed by manual push"));
        $(TestBenchElement.class).id("manaul-push").click();
        waitUntil(textToBePresentInElement(() -> getLastLog(),
                "3. Logged after 1s, followed by manual push"));
    }

}
