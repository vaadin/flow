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
package com.vaadin.flow.testutil;

import org.openqa.selenium.WebElement;

/**
 * Abstract base class for integration tests.
 *
 * @since 1.0
 */
public class AbstractComponentIT extends ChromeBrowserTest {

    @Override
    public void checkIfServerAvailable() {
        // this check is not used for the component ITs
    }

    @Override
    protected String getTestPath() {
        TestPath annotation = getClass().getAnnotation(TestPath.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "The test class should be annotated with @TestPath");
        }

        String path = annotation.value();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    @Override
    protected int getDeploymentPort() {
        return 9998;
    }

    public String getProperty(WebElement element, String propertyName) {
        Object result = executeScript(
                "return arguments[0]." + propertyName + ";", element);
        return result == null ? null : String.valueOf(result);
    }
}
