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
package com.vaadin.flow.uitest.ui;

import com.vaadin.testbench.TestBenchElement;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

@NotThreadSafe
public class PreserveOnRefreshLiveReloadIT extends AbstractLiveReloadIT {

    @Test
    public void notificationShownWhenLoadingPreserveOnRefreshView() {
        open();

        TestBenchElement liveReload = $("vaadin-devmode-gizmo").first();
        Assert.assertNotNull(liveReload);
        WebElement messageDetails = liveReload.$("*")
                .attributeContains("class", "warning").first();
        Assert.assertTrue(
                messageDetails.getText().contains("@PreserveOnRefresh"));
    }

}
