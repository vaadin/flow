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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;

public class EventTargetIT extends AbstractEventDataIT {

    @Test
    public void clickOnChildElement_reportsChildAsEventTarget() {
        open();
        clickAndVerifyTarget("Child-0");
        clickAndVerifyTarget("Grandchild-00");
        clickAndVerifyTarget("Grandchild-09");
        clickAndVerifyTarget("Child-1");
        clickAndVerifyTarget("Grandchild-19");
        clickAndVerifyTarget("Grandchild-15");
        clickAndVerifyTarget("Grandchild-55");
        clickAndVerifyTarget("Child-5");
        final WebElement h3 = findElement(By.tagName("h3"));
        h3.click();
        verifyEventTargetString(h3.getText());

        clickAndVerifyTarget("Child-6");
        clickAndVerifyTarget("Child-5");
        clickAndVerifyTarget("Grandchild-99");
        clickAndVerifyTarget("Grandchild-98");
        clickAndVerifyTarget("Grandchild-98");
        clickAndVerifyTarget("Child-9");

        // click on source of the listener reports itself too
        clickAndVerifyTarget(AbstractEventDataView.VIEW_CONTAINER);
    }

    protected void verifyEventTargetString(String text) {
        Assert.assertEquals("Invalid event.target element reported", text,
                $(DivElement.class).id(EventTargetView.TARGET_ID).getText());
    }
}
