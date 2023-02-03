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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.webcomponent.devbuild.ExportedComponentOne.EXPORTED_ID_ONE;
import static com.vaadin.flow.webcomponent.devbuild.ExportedComponentTwo.EXPORTED_ID_TWO;

public class EmbeddedComponentDevBundleIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void embeddedComponent_expressBuild_componentRendered() {
        open();

        TestBenchElement exportedComponentOne = $("exported-component-one")
                .waitForFirst();

        TestBenchElement exportedComponentInner = exportedComponentOne
                .$(DivElement.class).id(EXPORTED_ID_ONE);

        TestBenchElement innerComponent = exportedComponentInner
                .$("inner-component").waitForFirst();
        Assert.assertTrue(innerComponent != null && innerComponent.getText()
                .equals("This is a component inside embedded web component"));

        TestBenchElement exportedComponentTwo = $("exported-component-two")
                .waitForFirst();

        TestBenchElement exportedComponentTwoInner = exportedComponentTwo
                .$(DivElement.class).id(EXPORTED_ID_TWO);

        TestBenchElement button = exportedComponentTwoInner
                .$(NativeButtonElement.class).waitForFirst();
        Assert.assertNotNull(button);

        checkLogsForErrors();
    }

}
