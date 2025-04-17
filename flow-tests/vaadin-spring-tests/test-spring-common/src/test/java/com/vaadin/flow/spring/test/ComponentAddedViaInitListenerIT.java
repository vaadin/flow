/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;
import org.openqa.selenium.By;

public class ComponentAddedViaInitListenerIT extends AbstractSpringTest {

    @Test
    public void componentAddedViaInitListenerIsLoaded() {
        open();

        TestBenchElement component = $("init-listener-component").first();
        TestBenchElement div = component.$("div").first();
        Assert.assertEquals("Init Listener Component", div.getText());

        // Ensure the class name set by @EventListener style listener is there
        getDriver().findElement(By.cssSelector(".event-listener-was-here"));
    }

    @Override
    protected String getTestPath() {
        return "/";
    }
}
