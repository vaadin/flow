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
package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class SubContextIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context/";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("sub")));
    }

}
