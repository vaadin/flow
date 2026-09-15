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
package com.vaadin.flow.test;

import java.util.Arrays;
import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.testcategory.ChromeTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.annotations.BrowserConfiguration;
import com.vaadin.testbench.parallel.Browser;
import com.vaadin.testbench.parallel.BrowserUtil;

/**
 * Simplified chrome test that doesn't handle view/IT class paths. Uses Jupiter
 * API
 */
@Category(ChromeTests.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({ ScreenshotsOnFailureExtension.class })
@QuarkusTestResource(VaadinSystemPropertiesPropagator.class)
public abstract class AbstractChromeIT extends ChromeBrowserTest {

    @AfterEach
    public void tearDown() {
        getDriver().quit();
    }

    @BeforeAll
    public void setCapabilities() {
        setDesiredCapabilities(Browser.CHROME.getDesiredCapabilities());
    }

    @BeforeEach
    public void beforeTest() throws Exception {
        setup();
        checkIfServerAvailable();
    }

    @Override
    @BrowserConfiguration
    public List<DesiredCapabilities> getBrowsersToTest() {
        return Arrays.asList(BrowserUtil.chrome());
    }

}
