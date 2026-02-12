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
package com.vaadin.flow.server.webcomponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyConfigurationImplTest {

    PropertyConfigurationImpl<MyComponent, Integer> intPropertyConf;

    @BeforeEach
    public void init() {
        intPropertyConf = new PropertyConfigurationImpl<>(MyComponent.class,
                "int", Integer.class, 1);
    }

    @Test
    public void onChange() {
        intPropertyConf.onChange(MyComponent::setInt);

        MyComponent myComponent = new MyComponent();

        intPropertyConf.getOnChangeHandler().accept(myComponent, 5);

        assertEquals(5, myComponent.value,
                "onChangeHandler should have been set and value " + "updated");
    }

    @Test
    public void onChange_throwsIfCalledTwice() {
        assertThrows(IllegalStateException.class, () -> {
            intPropertyConf.onChange(MyComponent::setInt);
            intPropertyConf.onChange(MyComponent::setInt);
        });
    }

    @Test
    public void readOnly() {
        intPropertyConf.readOnly();

        PropertyData<Integer> data = intPropertyConf.getPropertyData();

        // verify default value for completeness
        assertEquals(1, (int) data.getDefaultValue(), "default value is 1");

        assertTrue(data.isReadOnly(),
                "read-only flag should have been set to true");
    }

    @Tag("for-reasons")
    private static final class MyComponent extends Component {
        private int value;

        public void setInt(int newValue) {
            this.value = newValue;
        }
    }
}
