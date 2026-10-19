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
package com.vaadin.flow.test.dom;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(NamespacedElementsView.class)
public class NamespacedElementsIT extends AbstractDefaultIT {

    @BrowserTest
    public void checkNamespace() {
        open();

        String svgNamespace = getNameSpace(findElement(By.tagName("rect")));
        assertTrue(svgNamespace.toLowerCase().contains("svg"),
                "Namespace should contain svg");

        String mathNamespace = getNameSpace(findElement(By.tagName("math")));
        assertTrue(mathNamespace.toLowerCase().contains("mathml"),
                "Namespace should contain mathml");

        String paragraphNamespace = getNameSpace(findElement(By.tagName("p")));
        assertFalse(paragraphNamespace.toLowerCase().contains("svg"),
                "Default NS should not contain svg");

    }

    private static String getNameSpace(WebElement element) {
        return element.getDomProperty("namespaceURI");
    }
}
