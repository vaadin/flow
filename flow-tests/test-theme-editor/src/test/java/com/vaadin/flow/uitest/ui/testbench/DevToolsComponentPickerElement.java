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
package com.vaadin.flow.uitest.ui.testbench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("vaadin-dev-tools-component-picker")
public class DevToolsComponentPickerElement extends TestBenchElement {
    public List<String> getOptions() {
        TestBenchElement optionsContainer = getOptionsContainer();
        return optionsContainer.$("div").all().stream()
                .map(d -> d.getText().strip()).collect(Collectors.toList());
    }

    public String getSelectedOption() {
        TestBenchElement optionsContainer = getOptionsContainer();
        return optionsContainer.$("div").attribute("class", "selected")
                .waitForFirst().getText().strip();
    }

    private TestBenchElement getOptionsContainer() {
        TestBenchElement componentsInfo = this.$("div")
                .attributeContains("class", "component-picker-components-info")
                .first();
        TestBenchElement optionsContainer = componentsInfo.$("div")
                .waitForFirst();
        return optionsContainer;
    }

    public void moveToElement(WebElement findElement) {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> details = new HashMap<>();
        details.put("target", findElement);

        data.put("detail", details);

        TestBenchElement shim = this.$("vaadin-dev-tools-shim").first();
        shim.dispatchEvent("shim-mousemove", data);
    }

    public void moveToOption(String option) {
        List<String> options = getOptions();
        if (!options.contains(option)) {
            throw new RuntimeException(
                    String.format("'%s' is not a valid option", option));
        }
        while (!getSelectedOption().equals(option)) {
            moveUp();
        }
    }

    public void selectOption(String option) {
        moveToOption(option);
        pressEnter();
    }

    private void pressEnter() {
        new Actions(getDriver()).keyDown(Keys.ENTER).perform();
    }

    private void moveUp() {
        new Actions(getDriver()).keyDown(Keys.ARROW_UP).perform();
        new Actions(getDriver()).keyUp(Keys.ARROW_UP).perform();
    }
}
