/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.webcomponent.devbuild;

import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.webcomponent.devbuild.ExportedComponent.EXPORTED_ID;
import static com.vaadin.flow.webcomponent.devbuild.ExportedComponent.INNER_COMPONENT_ID;

public class EmbeddedComponentDevBundleIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void embeddedComponent_expressBuild_componentRendered() {
        open();

        waitUntil(driver -> {
            TestBenchElement exportedOuterComponent = $("exported-component")
                    .id("exported-outer");
            TestBenchElement embeddedComponent = exportedOuterComponent
                    .$(DivElement.class).id(EXPORTED_ID);
            DivElement innerComponent = embeddedComponent.$(DivElement.class)
                    .id(INNER_COMPONENT_ID);
            return innerComponent != null && innerComponent.getText().equals(
                    "This is a component inside embedded web component");
        });

        checkLogsForErrors();
    }

}
