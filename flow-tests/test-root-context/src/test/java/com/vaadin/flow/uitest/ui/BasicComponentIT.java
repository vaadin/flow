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
import org.openqa.selenium.WebElement;

public class BasicComponentIT extends AbstractBasicElementComponentIT {

    @Test
    public void tagsInText() {
        open();
        WebElement root = findElement(By.id("root"));

        // Selenium does not support text nodes...
        Assert.assertEquals(
                BasicComponentView.TEXT + "\n" + BasicComponentView.DIV_TEXT
                        + "\n" + BasicComponentView.BUTTON_TEXT,
                root.getText());
    }
}
