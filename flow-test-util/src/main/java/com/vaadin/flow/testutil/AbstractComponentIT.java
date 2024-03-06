/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
