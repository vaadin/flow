/*
 * Copyright 2000-2026 Vaadin Ltd.
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
        // Swagger UI must render the application's own OpenAPI definition. The
        // page must not fall back to the bundled petstore demo, which would
        // mean the spec was not reachable (e.g. wrong URL behind a proxy) and
        // would make the test depend on external network availability.
        waitUntil(driver -> driver.getPageSource()
                .contains("OpenAPI definition"));
    }
}
