/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.io.File;

import org.junit.Before;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.parallel.Browser;

/**
 * Base class for TestBench tests for some dedicated browser which is specified
 * via {@link #getBrowser()}.
 * <p>
 * By default Chrome is used.
 * <p>
 * It is required to set system property with path to the driver to be able to
 * run the test.
 * 
 * @author Vaadin Ltd
 *
 */
public class SingleBrowserTest extends ViewOrUITest {

    /**
     * Setup the driver for the active browser.
     */
    @Before
    public void setupDriver() {
        switch (getBrowser()) {
        case FIREFOX:
            setDriver(new FirefoxDriver(DesiredCapabilities.firefox()));
            break;
        case CHROME:
            String chrome = System.getenv("CHROME_BIN");
            System.out.println("xxxxxxxxxxx " + chrome);
            ChromeOptions options = new ChromeOptions();
            if (chrome != null) {
                options.setBinary(new File(chrome));
            }
            setDriver(new ChromeDriver(options));
            break;
        case PHANTOMJS:
        default:
            setupPhantomJsDriver();
            break;
        }
    }

    protected Browser getBrowser() {
        return Browser.CHROME;
    }

}
