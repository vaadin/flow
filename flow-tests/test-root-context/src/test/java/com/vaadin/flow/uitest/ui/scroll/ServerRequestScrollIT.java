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
package com.vaadin.flow.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ServerRequestScrollIT extends ChromeBrowserTest {

    @Test
    public void scrollPositionIsTheSameAfterServerRequest() {
        open();

        WebElement button = $("server-request").id("template")
                .$(NativeButtonElement.class).first();

        int y = button.getLocation().getY();

        scrollBy(0, y);

        int scrollY = getScrollY();

        Assert.assertTrue(scrollY > 0);

        button.click();

        Assert.assertEquals(
                "Scroll position after the server request is changed", scrollY,
                getScrollY());
    }
}
