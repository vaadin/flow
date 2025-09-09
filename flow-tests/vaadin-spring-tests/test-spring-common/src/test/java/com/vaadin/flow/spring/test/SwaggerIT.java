package com.vaadin.flow.spring.test;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("Until Jackson 3 compatible swagger released")
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
