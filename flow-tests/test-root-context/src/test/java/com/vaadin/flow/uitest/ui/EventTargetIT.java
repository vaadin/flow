/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class EventTargetIT extends ChromeBrowserTest {

    @Test
    public void clickOnChildElement_reportsChildAsEventTarget() {
        open();
        clickAndVerify("Child-0");
        clickAndVerify("Grandchild-00");
        clickAndVerify("Grandchild-09");
        clickAndVerify("Child-1");
        clickAndVerify("Grandchild-19");
        clickAndVerify("Grandchild-15");
        clickAndVerify("Grandchild-55");
        clickAndVerify("Child-5");
        final WebElement h3 = findElement(By.tagName("h3"));
        h3.click();
        verifyEventTargetString(h3.getText());

        clickAndVerify("Child-6");
        clickAndVerify("Child-5");
        clickAndVerify("Grandchild-99");
        clickAndVerify("Grandchild-98");
        clickAndVerify("Grandchild-98");
        clickAndVerify("Child-9");

        // click on source of the listener reports itself too
        clickAndVerify("container");
    }

    private void clickAndVerify(String id) {
        final WebElement element = findElement(By.id(id));
        element.click();

        verifyEventTargetString(id);
    }

    private void verifyEventTargetString(String text) {
        Assert.assertEquals("Invalid event.target element reported", text,
                $(DivElement.class).id(EventTargetView.TARGET_ID).getText());
    }
}
