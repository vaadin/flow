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
package com.vaadin.flow.quarkus.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.quarkus.it.javascript.JsDefinitionView;
import com.vaadin.flow.test.AbstractChromeIT;

@QuarkusIntegrationTest
class JsDefinitionIT extends AbstractChromeIT {

    @Override
    protected String getTestPath() {
        return "/js-definition";
    }

    @Test
    void runDefinition_resultReturned() {
        open();

        $(NativeButtonElement.class).id(JsDefinitionView.RUN_ID).click();

        SpanElement result = $(SpanElement.class)
                .id(JsDefinitionView.RESULT_ID);
        waitUntil(driver -> "a-1-true".equals(result.getText()));
    }
}
