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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Checks that push keeps working after the push connection has been idle for
 * longer than the server's idle timeouts, which close an idle websocket and
 * resume a suspended long polling request. The view is served by
 * {@link IdlePushChannelServlet}, which shortens those timeouts so that the
 * test does not have to wait for the five minute default.
 */
@Category(PushTests.class)
public abstract class IdlePushChannelIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/idle-push/" + URLEncoder.encode(getViewClass().getName(),
                StandardCharsets.UTF_8);
    }

    @Test
    public void longWaitBetweenActions() throws Exception {
        open();
        BasicPushIT.getIncrementButton(this).click();
        testBench().disableWaitForVaadin();
        waitUntil(driver -> BasicPushIT.getClientCounter(this) == 1);

        Thread.sleep(3 * IdlePushChannelServlet.IDLE_TIMEOUT_MS);

        BasicPushIT.getIncrementButton(this).click();
        waitUntil(driver -> BasicPushIT.getClientCounter(this) == 2);

        // The server can still push to the client
        BasicPushIT.getServerCounterStartButton(this).click();
        waitUntil(driver -> BasicPushIT.getServerCounter(this) > 0);
    }

}
