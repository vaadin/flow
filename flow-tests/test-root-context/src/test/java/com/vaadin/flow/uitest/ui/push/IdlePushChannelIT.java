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
package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(PushTests.class)
public abstract class IdlePushChannelIT extends ChromeBrowserTest {

    private static final int SEVEN_MINUTES_IN_MS = 7 * 60 * 1000;

    @Test
    public void longWaitBetweenActions() throws Exception {
        open();
        BasicPushIT.getIncrementButton(this).click();
        Assert.assertEquals(1, BasicPushIT.getClientCounter(this));
        Thread.sleep(SEVEN_MINUTES_IN_MS);
        BasicPushIT.getIncrementButton(this).click();
        Assert.assertEquals(2, BasicPushIT.getClientCounter(this));
    }

}
