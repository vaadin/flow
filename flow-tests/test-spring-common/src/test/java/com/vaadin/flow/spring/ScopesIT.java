/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 *
 */
public class ScopesIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void checkSessionScope() throws Exception {
        open();

        String prefix = "foo";
        String text = findElement(By.id("message")).getText();
        Assert.assertTrue(text.startsWith(prefix));

        String sessionId = text.substring(prefix.length());
        String uiId = findElement(By.id("ui-id")).getText();

        // open another ui
        open();

        text = findElement(By.id("message")).getText();
        Assert.assertTrue(text.startsWith(prefix));

        Assert.assertEquals(sessionId, text.substring(prefix.length()));
        // self check: it should be another UI
        Assert.assertNotEquals(uiId, findElement(By.id("ui-id")).getText());
    }

    @Test
    public void checkUiScope() throws Exception {
        getDriver().get(getTestURL() + "ui-scope");

        String mainId = findElement(By.id("main")).getText();

        String innerId = findElement(By.id("inner")).getText();

        Assert.assertEquals(mainId, innerId);

        // open another ui
        getDriver().get(getTestURL() + "ui-scope");

        String anotherMainId = findElement(By.id("main")).getText();

        Assert.assertNotEquals(mainId, anotherMainId);

        innerId = findElement(By.id("inner")).getText();

        Assert.assertEquals(anotherMainId, innerId);
    }
}
