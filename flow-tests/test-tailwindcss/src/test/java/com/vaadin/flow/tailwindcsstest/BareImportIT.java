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
package com.vaadin.flow.tailwindcsstest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BareImportIT extends ChromeBrowserTest {

    @Test
    public void bareCssImportResolvesInMetaInfResources() {
        open();
        waitForDevServer();

        var target = findElement(By.id("bare-import-target"));
        String color = target.getCssValue("color");
        Assert.assertEquals(
                "Bare CSS @import should resolve and apply green color",
                "rgba(0, 128, 0, 1)", color);
    }
}
