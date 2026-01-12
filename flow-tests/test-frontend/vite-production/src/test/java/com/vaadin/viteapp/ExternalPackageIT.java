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
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.viteapp.views.empty.MainView;

public class ExternalPackageIT extends ChromeBrowserTest {

    @Test
    public void packageOutsideNpmWorks() {
        getDriver().get(getRootURL());
        waitForDevServer();
        $(NativeButtonElement.class).id(MainView.OUTSIDE).click();
        Assert.assertEquals("It works - It works", $(ParagraphElement.class)
                .id(MainView.OUTSIDE_RESULT).getText());
    }
}
