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
package com.vaadin.flow.test.routing;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(AddQueryParamView.class)
public class AddQueryParamIT extends AbstractDefaultIT {

    @BrowserTest
    public void validateReactInUse() {
        open();

        $(NativeButtonElement.class).id(AddQueryParamView.PARAM_BUTTON_ID)
                .click();

        waitForElementPresent(By.id(AddQueryParamView.QUERY_ID));

        Assertions.assertEquals(
                $(DivElement.class).id(AddQueryParamView.QUERY_ID).getText(),
                getDriver().getCurrentUrl());
    }
}
