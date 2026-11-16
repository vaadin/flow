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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;

@Category({ PushTests.class, IgnoreOSGi.class })
public class ReconnectWebsocketXhrIT extends ReconnectTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return BasicPushWebsocketXhrView.class;
    }

    @Test
    public void lostPushConnectionIsReportedAndRecovered() throws IOException {
        disconnectProxy();
        // Nothing is sent to the server over XHR, so only the lost websocket
        // can reveal that the connection is gone
        waitForConnectionState("reconnecting");

        connectProxy();
        int counter = BasicPushIT.getServerCounter(this);
        try {
            waitUntil(driver -> BasicPushIT.getServerCounter(this) > counter,
                    30);
        } catch (TimeoutException e) {
            Assert.fail("Server push did not resume. Expected counter above "
                    + counter + ", Actual: "
                    + BasicPushIT.getServerCounter(this));
        }
        waitForConnectionState("connected");
    }

    private void waitForConnectionState(String expected) {
        try {
            waitUntil(driver -> expected.equals(getConnectionState()), 30);
        } catch (TimeoutException e) {
            Assert.fail("Connection state does not match. Expected: " + expected
                    + ", Actual: " + getConnectionState());
        }
    }

    private Object getConnectionState() {
        return executeScript("return window.Vaadin.connectionState.state");
    }

}
