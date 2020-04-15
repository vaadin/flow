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

package com.vaadin.flow.uitest.ui.frontend;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

// These tests are not parallelizable, nor should they be run at the same time
// as other tests in the same module, due to live-reload affecting the whole
// application
@NotThreadSafe
public class PreserveOnRefreshLiveReloadIT extends ChromeBrowserTest {

    private static final Lock lock = new ReentrantLock();

    @Before
    @Override
    public void setup() throws Exception {
        lock.lock();
        super.setup();
    }

    @After
    public void tearDown() {
        lock.unlock();
    }

    @Test
    public void notificationShownWhenLoadingPreserveOnRefreshView() {
        open();

        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));
        Assert.assertNotNull(liveReload);
        WebElement statusDescription = findInShadowRoot(liveReload,
                By.className("status-description")).get(0);
        Assert.assertTrue(
                statusDescription.getText().contains("@PreserveOnRefresh"));
    }

}
