/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.testutil;

import org.junit.Before;
import org.junit.Rule;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.testbench.TestBenchDriverProxy;

/**
 * Base class for testbench tests for PhantomJS.
 */
public class PhantomJSTest extends AbstractTestBenchTest {

    /**
     * The rule used for screenshot failures.
     */
    @Rule
    public ScreenshotOnFailureRule screenshotOnFailure = new ScreenshotOnFailureRule(
            this, true);

    /**
     * Setup the PhantomJS driver.
     */
    @Before
    public void setupDriver() {
        DesiredCapabilities cap = DesiredCapabilities.phantomjs();
        FixedPhantomJSDriver driver = new FixedPhantomJSDriver(cap);
        setDriver(driver);
        driver.setTestBenchDriverProxy((TestBenchDriverProxy) getDriver());
    }

}
