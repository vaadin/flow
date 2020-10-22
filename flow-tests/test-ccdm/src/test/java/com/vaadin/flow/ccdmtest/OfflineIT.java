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
 *
 */
package com.vaadin.flow.ccdmtest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class OfflineIT extends CCDMTest {

    @Test
    public void should_loadServerSideViewContent_when_notSimulatingOffline() {
        openVaadinRouter();

        findAnchor("serverview").click();

        Assert.assertTrue("Should load server side view content",
                isElementPresent(By.id("serverView")));
    }

    @Test
    public void should_notLoadServerSideViewContent_when_simulatingOffline() {
        openVaadinRouter();

        setSimulateOffline(true);
        findAnchor("serverview").click();

        Assert.assertFalse("Should not load server side view content",
                isElementPresent(By.id("serverView")));
        setSimulateOffline(false);
    }

}
