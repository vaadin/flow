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
package com.vaadin.flow.tests.components;

import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Abstract base class for integration tests.
 */
public abstract class AbstractComponentIT extends ChromeBrowserTest {

    private static final String IT_POSTFIX = "IT";

    @Override
    public void checkIfServerAvailable() {
    }

    @Override
    protected String getTestPath() {
        String viewName = getClass().getSimpleName();
        if (viewName.endsWith(IT_POSTFIX)) {
            return "/" + viewName.substring(0,
                    viewName.length() - IT_POSTFIX.length());
        }
        return "/" + viewName;
    }

    @Override
    protected int getDeploymentPort() {
        return 8080;
    }

    public String getProperty(WebElement element, String propertyName) {
        Object result = executeScript(
                "return arguments[0]." + propertyName + ";", element);
        return result == null ? null : String.valueOf(result);
    }
}
