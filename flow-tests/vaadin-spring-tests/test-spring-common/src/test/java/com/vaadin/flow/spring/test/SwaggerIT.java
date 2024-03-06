/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Test;

public class SwaggerIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/swagger-ui.html";
    }

    @Test
    public void swaggerUIShown() {
        open();
        waitUntil(
                driver -> driver.getPageSource().contains("OpenAPI definition")
                        || driver.getPageSource().contains("Swagger Petstore"));
    }
}
