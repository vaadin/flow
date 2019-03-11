/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.beans.IntrospectionException;

public class InputTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    public void setup() throws IntrospectionException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        whitelistProperty("valueChangeMode");
        whitelistProperty("valueChangeTimeout");
        super.setup();
    }

    @Override
    protected void addProperties() {
        addStringProperty("type", "text");
        // Object.class because of generics
        addProperty("value", Object.class, "", "foo", false, false);
        addOptionalStringProperty("placeholder");
    }

}
