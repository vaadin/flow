/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.PushConnectionTracker;

/**
 * The view keeps the default long-polling fallback, so this also checks that
 * push did not fall back.
 */
@Category({ PushTests.class, IgnoreOSGi.class })
public class BasicPushIT extends AbstractClientServerCounterIT {

    @Override
    protected void updateHeadlessChromeOptions(ChromeOptions chromeOptions) {
        PushConnectionTracker.enable(chromeOptions);
    }

    @Override
    @Test
    public void testServerInitiatedCommunication() throws InterruptedException {
        super.testServerInitiatedCommunication();

        new PushConnectionTracker(getDriver())
                .assertOnlyTransport(Transport.WEBSOCKET_XHR);
    }
}
