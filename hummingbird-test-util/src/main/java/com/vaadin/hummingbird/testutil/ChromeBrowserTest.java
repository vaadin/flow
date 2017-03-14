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

import java.util.List;

import org.junit.experimental.categories.Category;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.hummingbird.testcategory.ChromeTests;
import com.vaadin.testbench.parallel.Browser;

/**
 * Base class for TestBench tests to run locally in the Chrome browser instead
 * of Phantom JS.
 * <p>
 * It is required to set system property with path to the driver to be able to
 * run the test.
 * <p>
 * The test can be executed locally and on a test Hub. Chrome browser is used
 * only if test is executed locally. The test Hub target browsers can be
 * configured via {@link #getHubBrowsersToTest()}.
 * 
 * 
 * @author Vaadin Ltd
 *
 */
@Category(ChromeTests.class)
@LocalExecution(Browser.CHROME)
public class ChromeBrowserTest extends ViewOrUITest {

    @Override
    protected List<DesiredCapabilities> getHubBrowsersToTest() {
        /*
         * Let's use only Chrome browser for now:
         * 
         * - phantom JS should be excluded for the ChromeBrowserTest test
         * because phantom JS doesn't support polymer (the main reason to use
         * ChromeBrowserTest) - FF version (currently used on hub) is not
         * supported.
         */
        return getBrowserCapabilities(Browser.CHROME);
    }
}
