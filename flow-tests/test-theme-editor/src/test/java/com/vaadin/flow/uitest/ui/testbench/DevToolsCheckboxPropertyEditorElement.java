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

import java.util.Collections;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("vaadin-dev-tools-theme-checkbox-property-editor")
public class DevToolsCheckboxPropertyEditorElement extends TestBenchElement {
    public void setChecked(boolean checked) {
        TestBenchElement checkbox = this.$("input").first();
        checkbox.setProperty("checked", checked);
        checkbox.dispatchEvent("input",
                Collections.singletonMap("bubbles", true));
        checkbox.dispatchEvent("change",
                Collections.singletonMap("bubbles", true));
    }

    public String getValue() {
        TestBenchElement checkbox = this.$("input").first();
        return checkbox.getPropertyString("value");
    }

    public String getCheckedValue() {
        return getPropertyString("propertyMetadata", "checkedValue");
    }
}
