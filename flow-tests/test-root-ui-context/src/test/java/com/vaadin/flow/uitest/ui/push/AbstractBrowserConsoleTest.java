/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.List;
import java.util.logging.Level;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractBrowserConsoleTest extends ChromeBrowserTest {

    @Override
    protected ChromeOptions customizeChromeOptions(ChromeOptions options) {
        ChromeOptions opts = super.customizeChromeOptions(options);

        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);

        opts.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        return opts;
    }

    @Override
    protected List<DesiredCapabilities> customizeCapabilities(
            List<DesiredCapabilities> capabilities) {
        List<DesiredCapabilities> caps = super.customizeCapabilities(
                capabilities);
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        caps.forEach(cap -> cap.setCapability(CapabilityType.LOGGING_PREFS,
                logPrefs));
        return caps;
    }

}
