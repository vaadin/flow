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
package com.vaadin.flow.test.devmode;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(InfoView.class)
public class InfoIT extends AbstractDefaultIT {

    @BrowserTest
    public void nonProductionModeServlet() {
        open();

        List<String> texts = getInfoTexts();

        assertEquals("false", getInfoValue(texts, "Production mode"));
        // The values are grouped under section headers
        assertTrue(texts.contains("Deployment configuration"),
                "The deployment configuration section header is missing: "
                        + texts);
    }

    private List<String> getInfoTexts() {
        return findElement(By.className("infoContainer"))
                .findElements(By.tagName("div")).stream()
                .map(WebElement::getText).toList();
    }

    private String getInfoValue(List<String> texts, String name) {
        String prefix = name + ": ";
        return texts.stream().filter(text -> text.startsWith(prefix))
                .findFirst().map(text -> text.substring(prefix.length()))
                .orElseThrow(() -> new AssertionError(
                        "No info row starts with '" + prefix + "': " + texts));
    }
}
