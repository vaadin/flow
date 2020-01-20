/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.BrowserUtil;

public class BodyScrollIT extends ChromeBrowserTest {

    @Test
    public void noScrollAttributeForBody() {
        open();

        String scrollAttribute = findElement(By.tagName("body"))
                .getAttribute("scroll");

        if (BrowserUtil.isIE(getDesiredCapabilities())) {
            Assert.assertTrue("The 'scroll' attribute of body should be empty",
                    scrollAttribute.isEmpty());
        } else {
            Assert.assertNull("Body should not have 'scroll' attribute",
                    scrollAttribute);
        }
    }
}
