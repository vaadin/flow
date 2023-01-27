/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.defaultbundle;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DefaultDevBundleThemeIT extends ChromeBrowserTest {

    @Test
    public void serveThemeAssetsInExpressBuildMode_assetsAreInDefaultBundle_serveFromDefaultBundle() {
        open();

        // line-awesome assets are served from default dev bundle
        waitForElementPresent(By.className("la-cat"));

        File baseDir = new File(System.getProperty("user.dir", "."));
        File devBundle = new File(baseDir, "src/main/dev-bundle");

        // shouldn't create a dev-bundle
        Assert.assertFalse(devBundle.exists());

        checkLogsForErrors();
    }

}
