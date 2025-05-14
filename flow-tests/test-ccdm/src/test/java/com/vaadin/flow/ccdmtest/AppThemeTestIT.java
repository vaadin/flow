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
package com.vaadin.flow.ccdmtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

public class AppThemeTestIT extends CCDMTest {
    private static final String BLUE_RGBA = "rgba(0, 0, 255, 1)";
    private static final String RED_RGBA = "rgba(255, 0, 0, 1)";

    @Before
    public void setUp() {
        openVaadinRouter();
    }

    @Test
    public void should_apply_AppTheme_on_clientSideView() {
        findAnchor("client-view").click();
        Assert.assertEquals(RED_RGBA,
                findElement(By.id("clientView")).getCssValue("color"));
    }

    @Test
    public void should_apply_AppTheme_on_serverSideView() {
        findAnchor("serverview").click();
        Assert.assertEquals(BLUE_RGBA,
                findElement(By.id("serverView")).getCssValue("color"));
    }
}
