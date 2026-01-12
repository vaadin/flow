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
package com.vaadin.flow.component.html.testbench;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NativeDetailsElementIT extends ChromeBrowserTest {

    private NativeDetailsElement details;
    private DivElement log;
    private NativeButtonElement button;

    @Test
    public void openDetails() {
        prepareTest(false);

        details.setProperty("open", true);
        Assert.assertEquals("Toggle event number '1' is 'true'", log.getText());
    }

    @Test
    public void openAndCloseDetails() {
        prepareTest(false);

        details.setProperty("open", true);
        Assert.assertEquals("Toggle event number '1' is 'true'", log.getText());

        details.setProperty("open", false);
        Assert.assertEquals("Toggle event number '2' is 'false'",
                log.getText());
    }

    @Test
    public void closingAlreadyClosedDetails() {
        prepareTest(false);

        details.setProperty("open", false);
        // Event should not be triggered, because details open property
        // defaults to false
        Assert.assertEquals("", log.getText());
    }

    @Test
    public void openAndCloseDetailsFromOtherServerSideComponent() {
        prepareTest(false);

        button.click();
        Assert.assertEquals("Toggle event number '1' is 'true'", log.getText());

        button.click();
        Assert.assertEquals("Toggle event number '2' is 'false'",
                log.getText());
    }

    @Test
    public void toggleNativeDetailsWithTestBenchElement() {
        prepareTest(false);

        details.toggle();
        Assert.assertEquals("Toggle event number '1' is 'true'", log.getText());

        details.toggle();
        Assert.assertEquals("Toggle event number '2' is 'false'",
                log.getText());
    }

    @Test
    public void openDetailsFromServerSideOnInitialRendering() {
        prepareTest(true);

        // Event should be triggered once, because details was already opened on
        // server side
        // triggering the toggle event.
        Assert.assertEquals("Toggle event number '1' is 'true'", log.getText());

        details.setProperty("open", true);
        // Event should not be triggered again, because details was already
        // opened.
        Assert.assertEquals("Toggle event number '1' is 'true'", log.getText());
    }

    private void prepareTest(boolean detailsOpen) {
        getDriver().get(
                "http://localhost:8888/Details" + (detailsOpen ? "/open" : ""));
        details = $(NativeDetailsElement.class).id("details");
        log = $(DivElement.class).id("log");
        button = $(NativeButtonElement.class).id("btn");
    }
}
