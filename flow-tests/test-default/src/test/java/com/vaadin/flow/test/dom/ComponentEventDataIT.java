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
package com.vaadin.flow.test.dom;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static com.vaadin.flow.test.dom.AbstractEventDataView.EMPTY_VALUE;

@TestFor(ComponentEventDataView.class)
public class ComponentEventDataIT extends AbstractEventDataIT {

    @BrowserTest
    public void clickElement_reportsComponentAndElementEventData() {
        open();

        clickAndVerifyTarget("Child-0");
        verifyDirectChild("Child-0");
        verifyTargetFirstChild("Grandchild-00");
        verifyHeader(EMPTY_VALUE);

        clickAndVerifyTarget("Grandchild-00");
        verifyDirectChild("Child-0");
        verifyTargetFirstChild(EMPTY_VALUE);
        verifyHeader(EMPTY_VALUE);

        clickAndVerifyTarget("Grandchild-99");
        verifyDirectChild("Child-9");
        verifyTargetFirstChild(EMPTY_VALUE);
        verifyHeader(EMPTY_VALUE);

        clickAndVerifyTarget("Child-9");
        verifyDirectChild("Child-9");
        verifyTargetFirstChild("Grandchild-90");
        verifyHeader(EMPTY_VALUE);
    }

    @BrowserTest
    public void clickConcreteComponent_mapsToCorrectComponentType() {
        open();

        final WebElement h3 = findElement(By.tagName("h3"));
        h3.click();
        verifyEventTargetString(h3.getText());
        verifyDirectChild(EMPTY_VALUE); // this is how the test code works
        verifyTargetFirstChild(EMPTY_VALUE);
        verifyHeader(AbstractEventDataView.HEADER);
    }

    private void verifyHeader(String text) {
        Assertions.assertEquals(
                text, $(DivElement.class)
                        .id(ComponentEventDataView.HEADER_CLICKED).getText(),
                "Invalid header reported");
    }

    private void verifyTargetFirstChild(String text) {
        Assertions.assertEquals(text,
                $(DivElement.class).id(ComponentEventDataView.FIRST_CHILD)
                        .getText(),
                "Invalid event.target.children[0] element reported");
    }

    private void verifyDirectChild(String text) {
        // this is just for making sure it is possible to do this
        Assertions.assertEquals(
                text, $(DivElement.class)
                        .id(ComponentEventDataView.CHILD_COMPONENT).getText(),
                "Invalid direct target component reported");
    }

    @Override
    protected void verifyEventTargetString(String text) {

    }
}
